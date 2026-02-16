/** Simple structured logger */
const isDev = import.meta.env.DEV;

export const logger = {
  info: (message: string, data?: unknown) => {
    if (isDev) console.log(`[INFO] ${message}`, data ?? '');
  },
  warn: (message: string, data?: unknown) => {
    console.warn(`[WARN] ${message}`, data ?? '');
  },
  error: (message: string, data?: unknown) => {
    console.error(`[ERROR] ${message}`, data ?? '');
  },
  debug: (message: string, data?: unknown) => {
    if (isDev) console.debug(`[DEBUG] ${message}`, data ?? '');
  },
};
