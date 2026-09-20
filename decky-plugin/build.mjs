import * as esbuild from "esbuild";
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const manifest = JSON.parse(fs.readFileSync(path.resolve(__dirname, "plugin.json"), "utf-8"));

const deckyPlugin = {
  name: "decky-globals",
  setup(build) {
    build.onResolve({ filter: /^@decky\/manifest$/ }, (args) => ({
      path: args.path,
      namespace: "decky-manifest",
    }));
    build.onLoad({ filter: /.*/, namespace: "decky-manifest" }, () => ({
      contents: `export default ${JSON.stringify(manifest)};`,
      loader: "js",
    }));

    const externals = {
      react: "window.SP_REACT",
      "react-dom": "window.SP_REACTDOM",
      "@decky/ui": "window.DFL",
    };

    for (const [mod, globalVar] of Object.entries(externals)) {
      const filter = new RegExp(`^${mod}$`);
      build.onResolve({ filter }, (args) => ({
        path: args.path,
        namespace: `external-${mod}`,
      }));
      build.onLoad({ filter: /.*/, namespace: `external-${mod}` }, () => ({
        contents: `module.exports = ${globalVar};`,
        loader: "js",
      }));
    }
  },
};

await esbuild.build({
  entryPoints: [path.resolve(__dirname, "src/index.tsx")],
  bundle: true,
  outfile: path.resolve(__dirname, "dist/index.js"),
  format: "esm",
  target: "es2020",
  jsx: "transform",
  jsxFactory: "window.SP_REACT.createElement",
  jsxFragment: "window.SP_REACT.Fragment",
  plugins: [deckyPlugin],
});

console.log("Built decky-plugin/dist/index.js");
