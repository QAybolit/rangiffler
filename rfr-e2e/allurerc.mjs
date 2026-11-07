import { existsSync, readFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const results = process.env.ALLURE_RESULTS_DIR ?? resolve(here, "build/allure-results");
const file = join(results, "environment.properties");

const variables = existsSync(file)
    ? Object.fromEntries(
          readFileSync(file, "utf8")
              .split("\n")
              .filter((line) => line.includes("="))
              .map((line) => {
                  const at = line.indexOf("=");
                  return [line.slice(0, at), line.slice(at + 1)];
              }),
      )
    : {};

export default {
    name: "Rangiffler E2E",
    variables,
    environments: {
        local: {
            name: "Local stand",
            matcher: () => true,
            variables,
        },
    },
    plugins: {
        awesome: {
            options: {
                singleFile: false,
            },
        },
    },
};
