export const categories = [
  "FOOD",
  "HOUSING",
  "TRANSPORT",
  "HEALTH",
  "EDUCATION",
  "ENTERTAINMENT",
  "SALARY",
  "OTHER",
] as const;
export type Category = (typeof categories)[number];
export type OperationType = "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";
export interface User {
  id: string;
  email: string;
  displayName: string;
}
export interface Account {
  id: string;
  name: string;
  currency: string;
  balance: string;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
}
export interface Entry {
  id: string;
  accountId: string;
  accountName: string;
  operationId: string;
  type: OperationType;
  direction: "CREDIT" | "DEBIT";
  amount: string;
  category: Category;
  description: string;
  createdAt: string;
}
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export interface Dashboard {
  totalBalance: string;
  income: string;
  expenses: string;
  transactionCount: number;
  balanceSeries: { date: string; balance: string }[];
  expensesByCategory: Partial<Record<Category, string>>;
  recentEntries: Entry[];
}
export interface Goal {
  id: string;
  name: string;
  targetAmount: string;
  currentAmount: string;
  dueDate: string | null;
  status: "ACTIVE" | "COMPLETED";
  version: number;
}
export interface Operation {
  id: string;
  type: OperationType;
  sourceAccountId: string | null;
  targetAccountId: string | null;
  amount: string;
  category: Category;
  description: string;
  createdAt: string;
  status?: "COMPLETED";
}
export interface Period {
  from: string;
  to: string;
}
