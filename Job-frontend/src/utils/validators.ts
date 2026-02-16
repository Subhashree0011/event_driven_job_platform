export const validators = {
  email: (value: string): boolean => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value),

  password: (value: string): boolean => value.length >= 8,

  url: (value: string): boolean => {
    try {
      new URL(value);
      return true;
    } catch {
      return false;
    }
  },

  required: (value: string): boolean => value.trim().length > 0,

  maxLength: (value: string, max: number): boolean => value.length <= max,

  minLength: (value: string, min: number): boolean => value.length >= min,

  salary: (min?: number, max?: number): boolean => {
    if (min !== undefined && max !== undefined) return min <= max;
    return true;
  },
};
