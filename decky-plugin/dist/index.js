var __create = Object.create;
var __defProp = Object.defineProperty;
var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
var __getOwnPropNames = Object.getOwnPropertyNames;
var __getProtoOf = Object.getPrototypeOf;
var __hasOwnProp = Object.prototype.hasOwnProperty;
var __commonJS = (cb, mod) => function __require() {
  return mod || (0, cb[__getOwnPropNames(cb)[0]])((mod = { exports: {} }).exports, mod), mod.exports;
};
var __copyProps = (to, from, except, desc) => {
  if (from && typeof from === "object" || typeof from === "function") {
    for (let key of __getOwnPropNames(from))
      if (!__hasOwnProp.call(to, key) && key !== except)
        __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
  }
  return to;
};
var __toESM = (mod, isNodeMode, target) => (target = mod != null ? __create(__getProtoOf(mod)) : {}, __copyProps(
  // If the importer is in node compatibility mode or this is not an ESM
  // file that has been converted to a CommonJS file using a Babel-
  // compatible transform (i.e. "__esModule" has not been set), then set
  // "default" to the CommonJS "module.exports" for node compatibility.
  isNodeMode || !mod || !mod.__esModule ? __defProp(target, "default", { value: mod, enumerable: true }) : target,
  mod
));

// external-@decky/ui:@decky/ui
var require_ui = __commonJS({
  "external-@decky/ui:@decky/ui"(exports, module) {
    module.exports = window.DFL;
  }
});

// external-react:react
var require_react = __commonJS({
  "external-react:react"(exports, module) {
    module.exports = window.SP_REACT;
  }
});

// src/index.tsx
var import_ui = __toESM(require_ui(), 1);

// decky-manifest:@decky/manifest
var manifest_default = { "name": "syncMyShit", "author": "Kyss007", "flags": [], "version": "2.1.1", "api_version": 1, "description": "Cloud save sync for Steam Deck \u2014 login in Desktop Mode, sync in Game Mode", "publish": { "tags": ["cloud", "save", "sync", "emulation", "gaming"], "description": "Automagic retro emulator cloud save sync across Steam Deck, Android, and PC.", "image": "https://raw.githubusercontent.com/Kyss007/syncMyShit/main/docs/banner.png" } };

// node_modules/@decky/api/dist/index.js
var manifest = manifest_default;
var API_VERSION = 2;
if (!manifest?.name) {
  throw new Error("[@decky/api]: Failed to find plugin manifest.");
}
var internalAPIConnection = window.__DECKY_SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED_deckyLoaderAPIInit;
if (!internalAPIConnection) {
  throw new Error("[@decky/api]: Failed to connect to the loader as as the loader API was not initialized. This is likely a bug in Decky Loader.");
}
var api;
try {
  api = internalAPIConnection.connect(API_VERSION, manifest.name);
} catch {
  api = internalAPIConnection.connect(1, manifest.name);
  console.warn(`[@decky/api] Requested API version ${API_VERSION} but the running loader only supports version 1. Some features may not work.`);
}
if (api._version != API_VERSION) {
  console.warn(`[@decky/api] Requested API version ${API_VERSION} but the running loader only supports version ${api._version}. Some features may not work.`);
}
var call = api.call;
var callable = api.callable;
var addEventListener = api.addEventListener;
var removeEventListener = api.removeEventListener;
var routerHook = api.routerHook;
var toaster = api.toaster;
var openFilePicker = api.openFilePicker;
var executeInTab = api.executeInTab;
var injectCssIntoTab = api.injectCssIntoTab;
var removeCssFromTab = api.removeCssFromTab;
var fetchNoCors = api.fetchNoCors;
var getExternalResourceURL = api.getExternalResourceURL;
var useQuickAccessVisible = api.useQuickAccessVisible;
var definePlugin = (fn) => {
  return (...args) => {
    return fn(...args);
  };
};

// src/index.tsx
var import_react3 = __toESM(require_react(), 1);

// node_modules/react-icons/lib/iconBase.mjs
var import_react2 = __toESM(require_react(), 1);

// node_modules/react-icons/lib/iconContext.mjs
var import_react = __toESM(require_react(), 1);
var DefaultContext = {
  color: void 0,
  size: void 0,
  className: void 0,
  style: void 0,
  attr: void 0
};
var IconContext = import_react.default.createContext && /* @__PURE__ */ import_react.default.createContext(DefaultContext);

// node_modules/react-icons/lib/iconBase.mjs
var _excluded = ["attr", "size", "title"];
function _objectWithoutProperties(e, t) {
  if (null == e) return {};
  var o, r, i = _objectWithoutPropertiesLoose(e, t);
  if (Object.getOwnPropertySymbols) {
    var n = Object.getOwnPropertySymbols(e);
    for (r = 0; r < n.length; r++) o = n[r], -1 === t.indexOf(o) && {}.propertyIsEnumerable.call(e, o) && (i[o] = e[o]);
  }
  return i;
}
function _objectWithoutPropertiesLoose(r, e) {
  if (null == r) return {};
  var t = {};
  for (var n in r) if ({}.hasOwnProperty.call(r, n)) {
    if (-1 !== e.indexOf(n)) continue;
    t[n] = r[n];
  }
  return t;
}
function _extends() {
  return _extends = Object.assign ? Object.assign.bind() : function(n) {
    for (var e = 1; e < arguments.length; e++) {
      var t = arguments[e];
      for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]);
    }
    return n;
  }, _extends.apply(null, arguments);
}
function ownKeys(e, r) {
  var t = Object.keys(e);
  if (Object.getOwnPropertySymbols) {
    var o = Object.getOwnPropertySymbols(e);
    r && (o = o.filter(function(r2) {
      return Object.getOwnPropertyDescriptor(e, r2).enumerable;
    })), t.push.apply(t, o);
  }
  return t;
}
function _objectSpread(e) {
  for (var r = 1; r < arguments.length; r++) {
    var t = null != arguments[r] ? arguments[r] : {};
    r % 2 ? ownKeys(Object(t), true).forEach(function(r2) {
      _defineProperty(e, r2, t[r2]);
    }) : Object.getOwnPropertyDescriptors ? Object.defineProperties(e, Object.getOwnPropertyDescriptors(t)) : ownKeys(Object(t)).forEach(function(r2) {
      Object.defineProperty(e, r2, Object.getOwnPropertyDescriptor(t, r2));
    });
  }
  return e;
}
function _defineProperty(e, r, t) {
  return (r = _toPropertyKey(r)) in e ? Object.defineProperty(e, r, { value: t, enumerable: true, configurable: true, writable: true }) : e[r] = t, e;
}
function _toPropertyKey(t) {
  var i = _toPrimitive(t, "string");
  return "symbol" == typeof i ? i : i + "";
}
function _toPrimitive(t, r) {
  if ("object" != typeof t || !t) return t;
  var e = t[Symbol.toPrimitive];
  if (void 0 !== e) {
    var i = e.call(t, r || "default");
    if ("object" != typeof i) return i;
    throw new TypeError("@@toPrimitive must return a primitive value.");
  }
  return ("string" === r ? String : Number)(t);
}
function Tree2Element(tree) {
  return tree && tree.map((node, i) => /* @__PURE__ */ import_react2.default.createElement(node.tag, _objectSpread({
    key: i
  }, node.attr), Tree2Element(node.child)));
}
function GenIcon(data) {
  return (props) => /* @__PURE__ */ import_react2.default.createElement(IconBase, _extends({
    attr: _objectSpread({}, data.attr)
  }, props), Tree2Element(data.child));
}
function IconBase(props) {
  var elem = (conf) => {
    var attr = props.attr, size = props.size, title = props.title, svgProps = _objectWithoutProperties(props, _excluded);
    var computedSize = size || conf.size || "1em";
    var className;
    if (conf.className) className = conf.className;
    if (props.className) className = (className ? className + " " : "") + props.className;
    return /* @__PURE__ */ import_react2.default.createElement("svg", _extends({
      stroke: "currentColor",
      fill: "currentColor",
      strokeWidth: "0"
    }, conf.attr, attr, svgProps, {
      className,
      style: _objectSpread(_objectSpread({
        color: props.color || conf.color
      }, conf.style), props.style),
      height: computedSize,
      width: computedSize,
      xmlns: "http://www.w3.org/2000/svg"
    }), title && /* @__PURE__ */ import_react2.default.createElement("title", null, title), props.children);
  };
  return IconContext !== void 0 ? /* @__PURE__ */ import_react2.default.createElement(IconContext.Consumer, null, (conf) => elem(conf)) : elem(DefaultContext);
}

// node_modules/react-icons/fa/index.mjs
function FaTrashAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 448 512" }, "child": [{ "tag": "path", "attr": { "d": "M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" }, "child": [] }] })(props);
}
function FaSyncAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M370.72 133.28C339.458 104.008 298.888 87.962 255.848 88c-77.458.068-144.328 53.178-162.791 126.85-1.344 5.363-6.122 9.15-11.651 9.15H24.103c-7.498 0-13.194-6.807-11.807-14.176C33.933 94.924 134.813 8 256 8c66.448 0 126.791 26.136 171.315 68.685L463.03 40.97C478.149 25.851 504 36.559 504 57.941V192c0 13.255-10.745 24-24 24H345.941c-21.382 0-32.09-25.851-16.971-40.971l41.75-41.749zM32 296h134.059c21.382 0 32.09 25.851 16.971 40.971l-41.75 41.75c31.262 29.273 71.835 45.319 114.876 45.28 77.418-.07 144.315-53.144 162.787-126.849 1.344-5.363 6.122-9.15 11.651-9.15h57.304c7.498 0 13.194 6.807 11.807 14.176C478.067 417.076 377.187 504 256 504c-66.448 0-126.791-26.136-171.315-68.685L48.97 471.03C33.851 486.149 8 475.441 8 454.059V320c0-13.255 10.745-24 24-24z" }, "child": [] }] })(props);
}
function FaSignOutAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z" }, "child": [] }] })(props);
}
function FaGamepad(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 640 512" }, "child": [{ "tag": "path", "attr": { "d": "M480.07 96H160a160 160 0 1 0 114.24 272h91.52A160 160 0 1 0 480.07 96zM248 268a12 12 0 0 1-12 12h-52v52a12 12 0 0 1-12 12h-24a12 12 0 0 1-12-12v-52H84a12 12 0 0 1-12-12v-24a12 12 0 0 1 12-12h52v-52a12 12 0 0 1 12-12h24a12 12 0 0 1 12 12v52h52a12 12 0 0 1 12 12zm216 76a40 40 0 1 1 40-40 40 40 0 0 1-40 40zm64-96a40 40 0 1 1 40-40 40 40 0 0 1-40 40z" }, "child": [] }] })(props);
}
function FaExclamationCircle(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zm-248 50c-25.405 0-46 20.595-46 46s20.595 46 46 46 46-20.595 46-46-20.595-46-46-46zm-43.673-165.346l7.418 136c.347 6.364 5.609 11.346 11.982 11.346h48.546c6.373 0 11.635-4.982 11.982-11.346l7.418-136c.375-6.874-5.098-12.654-11.982-12.654h-63.383c-6.884 0-12.356 5.78-11.981 12.654z" }, "child": [] }] })(props);
}
function FaCheckCircle(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M504 256c0 136.967-111.033 248-248 248S8 392.967 8 256 119.033 8 256 8s248 111.033 248 248zM227.314 387.314l184-184c6.248-6.248 6.248-16.379 0-22.627l-22.627-22.627c-6.248-6.249-16.379-6.249-22.628 0L216 308.118l-70.059-70.059c-6.248-6.248-16.379-6.248-22.628 0l-22.627 22.627c-6.248 6.248-6.248 16.379 0 22.627l104 104c6.249 6.249 16.379 6.249 22.628.001z" }, "child": [] }] })(props);
}

// src/index.tsx
var C = {
  cyan: "#2ee6ff",
  lime: "#3dff9a",
  coral: "#ff5c7a",
  ink: "#061018",
  panel: "rgba(13, 31, 42, 0.92)",
  line: "#1e3a48",
  muted: "#8aa4b0",
  text: "#eaf6fb",
  warn: "#ffc857"
};
var apiGetStatus = callable("get_status");
var apiSignOut = callable("sign_out");
var apiScan = callable("scan");
var apiRunSync = callable("run_sync");
var apiToggleAuto = callable("toggle_auto_sync");
var apiGetActivity = callable("get_activity");
var apiClearActivity = callable("clear_activity");
var LOGIN_CMD = "~/homebrew/plugins/syncMyShit/login-desktop.sh";
var Content = () => {
  const [email, setEmail] = (0, import_react3.useState)("");
  const [authed, setAuthed] = (0, import_react3.useState)(false);
  const [autoSync, setAutoSync] = (0, import_react3.useState)(true);
  const [monitoring, setMonitoring] = (0, import_react3.useState)(false);
  const [emulators, setEmulators] = (0, import_react3.useState)([]);
  const [totalSaves, setTotalSaves] = (0, import_react3.useState)(0);
  const [logs, setLogs] = (0, import_react3.useState)([]);
  const [syncing, setSyncing] = (0, import_react3.useState)(false);
  const [syncTarget, setSyncTarget] = (0, import_react3.useState)(null);
  const [version, setVersion] = (0, import_react3.useState)("2.1.1");
  const [error, setError] = (0, import_react3.useState)("");
  const refresh = (0, import_react3.useCallback)(async () => {
    try {
      const st = await apiGetStatus();
      if (!st || st.success === false) {
        setError(st?.error || "Backend error");
        setAuthed(false);
        return;
      }
      setError("");
      setAuthed(!!st.is_authenticated);
      setEmail(st.email || "");
      setAutoSync(!!st.auto_sync);
      setMonitoring(!!st.is_monitoring);
      setVersion(st.version || "2.1.1");
      try {
        const scan = await apiScan();
        if (scan.success) {
          setEmulators(scan.emulators || []);
          setTotalSaves(scan.total_saves || 0);
        }
      } catch (_) {
      }
      try {
        const act = await apiGetActivity();
        if (act.success) setLogs(act.logs || []);
      } catch (_) {
      }
    } catch (e) {
      setError(String(e?.message || e));
    }
  }, []);
  (0, import_react3.useEffect)(() => {
    refresh();
    const t = setInterval(refresh, 5e3);
    return () => clearInterval(t);
  }, [refresh]);
  const doSync = async (id) => {
    if (!authed) {
      toaster.toast({
        title: "Not signed in",
        body: "Login in Desktop Mode first (see instructions below).",
        duration: 5e3
      });
      return;
    }
    setSyncing(true);
    setSyncTarget(id || "all");
    try {
      const res = await apiRunSync(id || "");
      if (res.success) {
        toaster.toast({
          title: "Sync done",
          body: `\u2191 ${res.uploaded}  \u2193 ${res.downloaded}`,
          duration: 4500
        });
      } else {
        toaster.toast({ title: "Sync issue", body: res.error || "Failed", duration: 5e3 });
      }
      await refresh();
    } catch (e) {
      toaster.toast({ title: "Sync failed", body: String(e?.message || e), duration: 5e3 });
    } finally {
      setSyncing(false);
      setSyncTarget(null);
    }
  };
  const toggleAuto = async (v) => {
    setAutoSync(v);
    try {
      const res = await apiToggleAuto(v);
      setMonitoring(!!res.is_monitoring);
    } catch (_) {
    }
  };
  const signOut = async () => {
    try {
      await apiSignOut();
      setAuthed(false);
      setEmail("");
      toaster.toast({ title: "Signed out", body: "Tokens cleared", duration: 3e3 });
      await refresh();
    } catch (_) {
    }
  };
  return /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { paddingBottom: 12 } }, /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      style: {
        margin: "0 0 10px",
        padding: "14px 12px 12px",
        background: `linear-gradient(135deg, ${C.ink} 0%, #0a2a33 55%, #0a2418 100%)`,
        borderBottom: `2px solid ${C.cyan}`,
        fontFamily: '"Trebuchet MS", "Segoe UI", sans-serif'
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 22, fontWeight: 800, letterSpacing: "-0.03em", color: C.cyan } }, "syncMyShit"),
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 11, color: C.muted, marginTop: 4 } }, "Sync in Game Mode. Sign in from Desktop Mode.")
  ), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Google Drive" }, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      style: {
        width: "100%",
        padding: 12,
        background: C.panel,
        border: `1px solid ${C.line}`,
        borderLeft: `3px solid ${authed ? C.lime : C.warn}`
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", gap: 8, marginBottom: 4 } }, authed ? /* @__PURE__ */ window.SP_REACT.createElement(FaCheckCircle, { color: C.lime, size: 14 }) : /* @__PURE__ */ window.SP_REACT.createElement(FaExclamationCircle, { color: C.warn, size: 14 }), /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { fontWeight: 700, fontSize: 13, color: authed ? C.lime : C.warn } }, authed ? email || "Connected" : "Not signed in")),
    error && /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 11, color: C.coral, marginTop: 6 } }, error)
  )), !authed && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      style: {
        width: "100%",
        padding: 12,
        background: C.panel,
        border: `1px solid ${C.line}`,
        fontSize: 12,
        color: C.text,
        lineHeight: 1.5
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontWeight: 700, color: C.cyan, marginBottom: 8 } }, "Sign in once (Desktop Mode)"),
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { color: C.muted, marginBottom: 8 } }, "1. Switch to ", /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.text } }, "Desktop Mode"), /* @__PURE__ */ window.SP_REACT.createElement("br", null), "2. Open ", /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.text } }, "Konsole"), /* @__PURE__ */ window.SP_REACT.createElement("br", null), "3. Run:"),
    /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          fontFamily: "Consolas, monospace",
          fontSize: 11,
          color: C.lime,
          background: "#021018",
          padding: "10px 8px",
          wordBreak: "break-all",
          marginBottom: 8
        }
      },
      LOGIN_CMD
    ),
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { color: C.muted } }, "First run asks for a Google ", /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.text } }, "Desktop"), " OAuth Client ID (not the Android one \u2014 that causes invalid_request).", /* @__PURE__ */ window.SP_REACT.createElement("br", null), "Then finish Google in the browser and return here.")
  )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.ButtonItem, { layout: "below", onClick: refresh }, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: 6 } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSyncAlt, { size: 12 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, "Refresh status")))), authed && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.ButtonItem, { layout: "below", onClick: signOut }, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: 6 } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSignOutAlt, { size: 12 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, "Sign out"))))), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Sync" }, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.ButtonItem, { layout: "below", onClick: () => doSync(), disabled: syncing || !authed }, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: 8 } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSyncAlt, { className: syncing && syncTarget === "all" ? "fa-spin" : "" }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, syncing && syncTarget === "all" ? "Syncing\u2026" : !authed ? "Sign in (Desktop Mode) to sync" : "\u26A1 Sync All Saves")))), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    import_ui.ToggleField,
    {
      label: "Auto-sync on emulator exit",
      description: monitoring ? "Watching emulator processes" : autoSync ? "On when linked" : "Off",
      checked: autoSync,
      onChange: toggleAuto
    }
  )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.Field, { label: "Detected saves", description: `${emulators.length} emulators` }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.cyan, fontWeight: 700 } }, totalSaves)))), emulators.length > 0 && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Emulators" }, emulators.map((emu) => /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, { key: emu.id }, /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      style: {
        width: "100%",
        display: "flex",
        alignItems: "center",
        gap: 8,
        padding: "6px 0",
        borderBottom: `1px solid ${C.line}`
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement(FaGamepad, { color: C.muted, size: 12, style: { flexShrink: 0 } }),
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { flex: 1, minWidth: 0 } }, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 12, fontWeight: 700, color: C.text } }, emu.name), /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 10, color: C.muted } }, emu.save_count, " file", emu.save_count === 1 ? "" : "s", " \xB7 ", emu.category)),
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.ButtonItem, { onClick: () => doSync(emu.id), disabled: syncing || !authed }, syncing && syncTarget === emu.id ? "\u2026" : "Sync")
  )))), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Activity" }, logs.length === 0 ? /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: 11, color: C.muted } }, "No activity yet.")) : logs.slice(0, 10).map((log, i) => /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, { key: `${log.timestamp}-${i}` }, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { width: "100%", fontSize: 11 } }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.muted } }, log.timestamp), " ", /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: log.status === "error" ? C.coral : C.lime } }, log.message)))), logs.length > 0 && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
    import_ui.ButtonItem,
    {
      layout: "below",
      onClick: async () => {
        await apiClearActivity();
        setLogs([]);
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: 6 } }, /* @__PURE__ */ window.SP_REACT.createElement(FaTrashAlt, { size: 11 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, "Clear activity"))
  ))), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "About" }, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.Field, { label: "Version", description: "Decky plugin" }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: C.cyan, fontWeight: 700 } }, "v", version)))));
};
var index_default = definePlugin(() => ({
  title: /* @__PURE__ */ window.SP_REACT.createElement("div", { className: import_ui.staticClasses.Title }, "syncMyShit"),
  content: /* @__PURE__ */ window.SP_REACT.createElement(Content, null),
  icon: /* @__PURE__ */ window.SP_REACT.createElement(FaGamepad, null)
}));
export {
  index_default as default
};
