/**
 * Texts used in many places (form errors, common buttons). They are functions
 * so that $localize runs when the text is shown, after the translations are loaded.
 */
export const Messages = {
  required: () => $localize`:@@validation.required:Required`,
  invalidEmail: () => $localize`:@@validation.email:Invalid email`,
};
