import { readFile, writeFile, mkdir } from "node:fs/promises";
import { dirname } from "node:path";
const tree = JSON.parse(
  await readFile(process.argv[2] || "target/dependencies.json", "utf8"),
);
const output = process.argv[3] || "target/dependency-audit.json";
const packages = new Map();
function visit(node) {
  if (node.groupId !== "com.ledgerflow")
    packages.set(`${node.groupId}:${node.artifactId}@${node.version}`, {
      name: `${node.groupId}:${node.artifactId}`,
      version: node.version,
      scope: node.scope,
    });
  for (const child of node.children || []) visit(child);
}
visit(tree);
const dependencies = [...packages.values()];
const response = await fetch("https://api.osv.dev/v1/querybatch", {
  method: "POST",
  headers: { "Content-Type": "application/json" },
  body: JSON.stringify({
    queries: dependencies.map((item) => ({
      package: { ecosystem: "Maven", name: item.name },
      version: item.version,
    })),
  }),
  signal: AbortSignal.timeout(30000),
});
if (!response.ok) throw new Error(`OSV unavailable: ${response.status}`);
const data = await response.json();
if (
  data.results?.length !== dependencies.length ||
  data.results.some((result) => result.next_page_token)
)
  throw new Error("Incomplete OSV response; do not claim an audit pass");
// Version-specific dispositions from docs/release-audit.md. Reassess on any
// addition of SSE fragments/XSLT views or change to the framework version.
const reviewed = new Set(["GHSA-j9f9-w8pj-32f8", "GHSA-pc63-qcmh-9cmg"]);
const findings = dependencies.map((item, index) => ({
  ...item,
  advisories: (data.results[index].vulns || []).map((advisory) => ({
    id: advisory.id,
    disposition:
      item.name === "org.springframework:spring-webmvc" &&
      item.version === "6.2.19" &&
      reviewed.has(advisory.id)
        ? "not_affected_reviewed"
        : "requires_review",
  })),
}));
const unreviewed = findings.flatMap((item) =>
  item.advisories
    .filter((advisory) => advisory.disposition === "requires_review")
    .map((advisory) => `${item.name}@${item.version}: ${advisory.id}`),
);
await mkdir(dirname(output), { recursive: true });
await writeFile(
  output,
  JSON.stringify(
    {
      queriedAt: new Date().toISOString(),
      source: "https://api.osv.dev/v1/querybatch",
      dependencyCount: dependencies.length,
      findings,
    },
    null,
    2,
  ),
);
console.log(
  JSON.stringify({
    dependencies: dependencies.length,
    advisories: findings.flatMap((item) => item.advisories).length,
    requiresReview: unreviewed.length,
  }),
);
if (unreviewed.length) {
  console.error(unreviewed.join("\n"));
  process.exitCode = 1;
}
