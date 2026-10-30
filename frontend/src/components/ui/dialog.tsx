import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
export function Dialog({
  open,
  onOpenChange,
  title,
  description,
  children,
  locked = false,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
  locked?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <DialogPrimitive.Root
      open={open}
      onOpenChange={locked ? () => {} : onOpenChange}
    >
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="dialog-overlay" />
        <DialogPrimitive.Content
          className="dialog-content"
          onPointerDownOutside={(event) => {
            if (locked) event.preventDefault();
          }}
          onEscapeKeyDown={(event) => {
            if (locked) event.preventDefault();
          }}
        >
          <div className="dialog-heading">
            <DialogPrimitive.Title>{title}</DialogPrimitive.Title>
            <DialogPrimitive.Close
              className="icon-button"
              aria-label={t("close")}
              disabled={locked}
            >
              <X size={19} />
            </DialogPrimitive.Close>
          </div>
          <DialogPrimitive.Description
            className={description ? "muted" : "sr-only"}
          >
            {description || title}
          </DialogPrimitive.Description>
          {children}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
