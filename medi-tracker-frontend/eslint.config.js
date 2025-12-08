import js from "@eslint/js";
import globals from "globals";
import pluginReact from "eslint-plugin-react";
import pluginReactHooks from "eslint-plugin-react-hooks";
import prettier from "eslint-plugin-prettier/recommended";

export default [
  js.configs.recommended,
  pluginReact.configs.flat.recommended,
  prettier,
  {
    files: ["**/*.{js,mjs,cjs,jsx}"],
    languageOptions: {
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": pluginReactHooks,
    },
    rules: {
      ...pluginReactHooks.configs.recommended.rules,
      "react/react-in-jsx-scope": "off", // Disable for React 17+
    },
    settings: {
      react: {
        version: "detect",
      },
    },
  },
];
