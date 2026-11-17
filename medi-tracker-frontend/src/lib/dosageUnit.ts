// Dose units the backend accepts, with friendlier labels for the picker
export const DOSAGE_UNITS: { value: string; label: string }[] = [
  { value: "TABLET", label: "Tablet / pill" },
  { value: "CAPSULE", label: "Capsule" },
  { value: "ML", label: "ml (liquid)" },
  { value: "MG", label: "mg" },
  { value: "MCG", label: "mcg" },
  { value: "DROP", label: "Drop" },
  { value: "PUFF", label: "Puff (inhaler / spray)" },
  { value: "PATCH", label: "Patch" },
];

/**
 * Guesses the dose unit from the FDA dosage form and route, e.g. "CAPSULE, GELATIN COATED" -> CAPSULE.
 * Returns null when nothing fits (creams, gels...), so the user picks.
 */
export function suggestDosageUnit(dosageForm?: string, route?: string): string | null {
  const form = (dosageForm || "").toUpperCase();
  const routes = (route || "").toUpperCase();
  if (!form) return null;

  if (form.includes("CAPSULE")) return "CAPSULE";
  if (/TABLET|PILL|LOZENGE|TROCHE|CAPLET/.test(form)) return "TABLET";
  if (form.includes("PATCH") || routes.includes("TRANSDERMAL")) return "PATCH";
  if (/AEROSOL|INHAL|SPRAY/.test(form) || routes.includes("RESPIRATORY")) return "PUFF";
  if (form.includes("DROP") || /OPHTHALMIC|OTIC/.test(routes)) return "DROP";
  // injections are drawn up in ml, even the ones that start as a powder
  if (form.includes("INJECTION")) return "ML";
  // granule packets are prescribed by weight (e.g. Prograf granules)
  if (form.includes("GRANULE")) return "MG";
  // powders mixed with water (kids' antibiotics) are measured as liquid
  if (/FOR (SOLUTION|SUSPENSION)/.test(form)) return "ML";
  if (form.includes("POWDER")) return "MG";
  if (/SOLUTION|SUSPENSION|SYRUP|ELIXIR|LIQUID|EMULSION|CONCENTRATE/.test(form)) return "ML";
  // creams, gels, kits, gases...: no sensible unit, let the user choose
  return null;
}

export function unitLabel(value: string) {
  return DOSAGE_UNITS.find((u) => u.value === value)?.label ?? value;
}
