import js from "@eslint/js";
import globals from "globals";
import pluginReact from "eslint-plugin-react";
import pluginReactHooks from "eslint-plugin-react-hooks";
import prettier from "eslint-plugin-prettier/recommended";
import pluginUnusedImports from "eslint-plugin-unused-imports";

export default [
  js.configs.recommended,
  pluginReact.configs.flat.recommended,
  prettier,
  {
    files: ["**/*.{js,mjs,cjs,jsx, }"],
    languageOptions: {
      globals: globals.browser,
    },
    plugins: {
      "react-hooks": pluginReactHooks,
      "unused-imports": pluginUnusedImports,
    },
    rules: {
      ...pluginReactHooks.configs.recommended.rules,
      "react/react-in-jsx-scope": "off", // Disable for React 17+

      "no-unused-vars": "off",
      "unused-imports/no-unused-imports": "error",

      "unused-imports/no-unused-vars": [
            "warn",
            {
              vars: "all",
              varsIgnorePattern: "^_",
              args: "after-used",
              argsIgnorePattern: "^_",
            },
          ],
    },
    settings: {
      react: {
        version: "detect",
      },
    },
  },
];
