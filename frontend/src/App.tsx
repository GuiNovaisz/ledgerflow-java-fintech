import { lazy, Suspense } from "react";
import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { useSession } from "./state";
import { Loading } from "./components/common";
import Layout from "./components/layout";
import { OperationProvider } from "./components/operation";
const Auth = lazy(() => import("./pages/auth"));
const Dashboard = lazy(() => import("./pages/dashboard"));
const Accounts = lazy(() => import("./pages/accounts"));
const AccountDetails = lazy(() =>
  import("./pages/accounts").then((module) => ({
    default: module.AccountDetailPage,
  })),
);
const Transactions = lazy(() => import("./pages/transactions"));
const Reports = lazy(() =>
  import("./pages/transactions").then((module) => ({
    default: module.ReportsPage,
  })),
);
const Goals = lazy(() => import("./pages/goals"));
const Preferences = lazy(() => import("./pages/preferences"));
const Categories = lazy(() => import("./pages/categories"));
function Protected() {
  const { user, loading } = useSession();
  return loading ? (
    <Loading />
  ) : user ? (
    <OperationProvider>
      <Outlet />
    </OperationProvider>
  ) : (
    <Navigate to="/login" replace />
  );
}
export default function App() {
  return (
    <Suspense fallback={<Loading />}>
      <Routes>
        <Route path="/login" element={<Auth />} />
        <Route path="/register" element={<Auth />} />
        <Route element={<Protected />}>
          <Route element={<Layout />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/accounts" element={<Accounts />} />
            <Route path="/accounts/:id" element={<AccountDetails />} />
            <Route path="/transactions" element={<Transactions />} />
            <Route path="/reports" element={<Reports />} />
            <Route path="/goals" element={<Goals />} />
            <Route path="/categories" element={<Categories />} />
            <Route path="/preferences" element={<Preferences />} />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  );
}
