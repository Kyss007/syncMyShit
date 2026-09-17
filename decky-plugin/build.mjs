import * as esbuild from "esbuild";
import fs from "fs";

const manifest = JSON.parse(fs.readFileSync("./plugin.json", "utf-8"));

const deckyPlugin = {
  name: "decky-globals",
  setup(build) {
    // Inject plugin manifest
    build.onResolve({ filter: /^@decky\/manifest$/ }, (args) => ({
      path: args.path,
      namespace: "decky-manifest",
    }));
    build.onLoad({ filter: /.*/, namespace: "decky-manifest" }, () => ({
      contents: `export default ${JSON.stringify(manifest)};`,
      loader: "js",
    }));

    // Route Decky runtime external globals
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
  entryPoints: ["src/index.tsx"],
  bundle: true,
  outfile: "dist/index.js",
  format: "esm",
  target: "es2020",
  jsx: "transform",
  jsxFactory: "window.SP_REACT.createElement",
  jsxFragment: "window.SP_REACT.Fragment",
  plugins: [deckyPlugin],
});

console.log("Built decky-plugin/dist/index.js successfully!");
