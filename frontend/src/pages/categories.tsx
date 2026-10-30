import { useTranslation } from "react-i18next";
import { Tags } from "lucide-react";
import { Link } from "react-router-dom";
import { categories } from "../types";
import { PageTitle } from "../components/common";
export default function CategoriesPage() {
  const { t } = useTranslation();
  return (
    <>
      <PageTitle title={t("categories")} caption={t("categoriesCaption")} />
      <div className="categories-grid">
        {categories.map((category, index) => (
          <Link
            to={"/transactions?category=" + category}
            key={category}
            className="panel category-card"
          >
            <span className={`account-icon tone-${index % 4}`}>
              <Tags size={23} />
            </span>
            <h2>{t(`categoryNames.${category}`)}</h2>
            <span className="text-link">{t("transactions")} ↗</span>
          </Link>
        ))}
      </div>
    </>
  );
}
