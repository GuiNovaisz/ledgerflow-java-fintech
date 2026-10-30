import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./i18n";
import "./styles.css";
import App from "./App";
import { PreferencesProvider, SessionProvider, NoticeProvider } from "./state";
const client = new QueryClient({
  defaultOptions: {
    queries: { staleTime: 20000, retry: 1, refetchOnWindowFocus: true },
    mutations: { retry: false },
  },
});
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={client}>
        <PreferencesProvider>
          <SessionProvider>
            <NoticeProvider>
              <App />
            </NoticeProvider>
          </SessionProvider>
        </PreferencesProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
