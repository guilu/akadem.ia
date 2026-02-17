/** @type {import('tailwindcss').Config} */
import flowbite from "flowbite/plugin";

export default {
  darkMode: "class",
  content: ["./src/**/*.{js,ts,jsx,tsx,html}", "./node_modules/flowbite-react/**/*.js"],
  theme: {
    extend: {
      colors: {
        bg: "rgb(var(--bg) / <alpha-value>)",
        text: "rgb(var(--text) / <alpha-value>)",
        primary: "rgb(var(--primary) / <alpha-value>)",
        secondary: "rgb(var(--secondary) / <alpha-value>)",
        accent: "rgb(var(--accent) / <alpha-value>)",
      },
    },
  },
  plugins: [flowbite],
}
