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

// decky-plugin/src/index.tsx
var import_ui = __toESM(require_ui(), 1);

// decky-manifest:@decky/manifest
var manifest_default = { "name": "syncMyShit", "author": "Kyss007", "flags": [], "version": "1.0.15", "api_version": 1, "description": "Automagic retro emulator cloud save sync for Steam Deck, Android, & PC", "publish": { "tags": ["cloud", "save", "sync", "emulation", "gaming"], "description": "Automagic retro emulator cloud save sync across Steam Deck, Android, and PC with zero save-loss protection.", "image": "https://raw.githubusercontent.com/Kyss007/syncMyShit/main/docs/banner.png" } };

// decky-plugin/node_modules/@decky/api/dist/index.js
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

// decky-plugin/src/index.tsx
var import_react3 = __toESM(require_react(), 1);

// decky-plugin/node_modules/react-icons/lib/iconBase.mjs
var import_react2 = __toESM(require_react(), 1);

// decky-plugin/node_modules/react-icons/lib/iconContext.mjs
var import_react = __toESM(require_react(), 1);
var DefaultContext = {
  color: void 0,
  size: void 0,
  className: void 0,
  style: void 0,
  attr: void 0
};
var IconContext = import_react.default.createContext && /* @__PURE__ */ import_react.default.createContext(DefaultContext);

// decky-plugin/node_modules/react-icons/lib/iconBase.mjs
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

// decky-plugin/node_modules/react-icons/fa/index.mjs
function FaGoogle(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 488 512" }, "child": [{ "tag": "path", "attr": { "d": "M488 261.8C488 403.3 391.1 504 248 504 110.8 504 0 393.2 0 256S110.8 8 248 8c66.8 0 123 24.5 166.3 64.9l-67.5 64.9C258.5 52.6 94.3 116.6 94.3 256c0 86.5 69.1 156.6 153.7 156.6 98.2 0 135-70.4 140.8-106.9H248v-85.3h236.1c2.3 12.7 3.9 24.9 3.9 41.4z" }, "child": [] }] })(props);
}
function FaTrashAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 448 512" }, "child": [{ "tag": "path", "attr": { "d": "M32 464a48 48 0 0 0 48 48h288a48 48 0 0 0 48-48V128H32zm272-256a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zm-96 0a16 16 0 0 1 32 0v224a16 16 0 0 1-32 0zM432 32H312l-9.4-18.7A24 24 0 0 0 281.1 0H166.8a23.72 23.72 0 0 0-21.4 13.3L136 32H16A16 16 0 0 0 0 48v32a16 16 0 0 0 16 16h416a16 16 0 0 0 16-16V48a16 16 0 0 0-16-16z" }, "child": [] }] })(props);
}
function FaSyncAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M370.72 133.28C339.458 104.008 298.888 87.962 255.848 88c-77.458.068-144.328 53.178-162.791 126.85-1.344 5.363-6.122 9.15-11.651 9.15H24.103c-7.498 0-13.194-6.807-11.807-14.176C33.933 94.924 134.813 8 256 8c66.448 0 126.791 26.136 171.315 68.685L463.03 40.97C478.149 25.851 504 36.559 504 57.941V192c0 13.255-10.745 24-24 24H345.941c-21.382 0-32.09-25.851-16.971-40.971l41.75-41.749zM32 296h134.059c21.382 0 32.09 25.851 16.971 40.971l-41.75 41.75c31.262 29.273 71.835 45.319 114.876 45.28 77.418-.07 144.315-53.144 162.787-126.849 1.344-5.363 6.122-9.15 11.651-9.15h57.304c7.498 0 13.194 6.807 11.807 14.176C478.067 417.076 377.187 504 256 504c-66.448 0-126.791-26.136-171.315-68.685L48.97 471.03C33.851 486.149 8 475.441 8 454.059V320c0-13.255 10.745-24 24-24z" }, "child": [] }] })(props);
}
function FaSignOutAlt(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M497 273L329 441c-15 15-41 4.5-41-17v-96H152c-13.3 0-24-10.7-24-24v-96c0-13.3 10.7-24 24-24h136V88c0-21.4 25.9-32 41-17l168 168c9.3 9.4 9.3 24.6 0 34zM192 436v-40c0-6.6-5.4-12-12-12H96c-17.7 0-32-14.3-32-32V160c0-17.7 14.3-32 32-32h84c6.6 0 12-5.4 12-12V76c0-6.6-5.4-12-12-12H96c-53 0-96 43-96 96v192c0 53 43 96 96 96h84c6.6 0 12-5.4 12-12z" }, "child": [] }] })(props);
}
function FaKey(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M512 176.001C512 273.203 433.202 352 336 352c-11.22 0-22.19-1.062-32.827-3.069l-24.012 27.014A23.999 23.999 0 0 1 261.223 384H224v40c0 13.255-10.745 24-24 24h-40v40c0 13.255-10.745 24-24 24H24c-13.255 0-24-10.745-24-24v-78.059c0-6.365 2.529-12.47 7.029-16.971l161.802-161.802C163.108 213.814 160 195.271 160 176 160 78.798 238.797.001 335.999 0 433.488-.001 512 78.511 512 176.001zM336 128c0 26.51 21.49 48 48 48s48-21.49 48-48-21.49-48-48-48-48 21.49-48 48z" }, "child": [] }] })(props);
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
function FaArrowAltCircleUp(props) {
  return GenIcon({ "tag": "svg", "attr": { "viewBox": "0 0 512 512" }, "child": [{ "tag": "path", "attr": { "d": "M8 256C8 119 119 8 256 8s248 111 248 248-111 248-248 248S8 393 8 256zm292 116V256h70.9c10.7 0 16.1-13 8.5-20.5L264.5 121.2c-4.7-4.7-12.2-4.7-16.9 0l-115 114.3c-7.6 7.6-2.2 20.5 8.5 20.5H212v116c0 6.6 5.4 12 12 12h64c6.6 0 12-5.4 12-12z" }, "child": [] }] })(props);
}

// decky-plugin/src/index.tsx
var apiGetStatus = callable("get_status");
var apiScanSaves = callable("scan_saves");
var apiRunSync = callable("run_sync");
var apiToggleWatcher = callable("toggle_watcher");
var apiGetRecentLogs = callable("get_recent_logs");
var apiClearLogs = callable("clear_logs");
var apiUpdatePlugin = callable("update_plugin");
var apiStartGoogleLogin = callable("start_google_login");
var apiSubmitAuthCode = callable("submit_auth_code");
var apiSignOutGoogle = callable("sign_out_google");
var formatTimestamp = (ts) => {
  if (!ts || ts <= 0) return "Never";
  const diffSec = Math.floor(Date.now() / 1e3 - ts);
  if (diffSec < 60) return "Just now";
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return new Date(ts * 1e3).toLocaleDateString();
};
var Content = () => {
  const [status, setStatus] = (0, import_react3.useState)(null);
  const [emulators, setEmulators] = (0, import_react3.useState)([]);
  const [totalSaves, setTotalSaves] = (0, import_react3.useState)(0);
  const [logs, setLogs] = (0, import_react3.useState)([]);
  const [syncing, setSyncing] = (0, import_react3.useState)(false);
  const [syncTargetId, setSyncTargetId] = (0, import_react3.useState)(null);
  const [updating, setUpdating] = (0, import_react3.useState)(false);
  const [loggingIn, setLoggingIn] = (0, import_react3.useState)(false);
  const [authUrl, setAuthUrl] = (0, import_react3.useState)("");
  const [manualCode, setManualCode] = (0, import_react3.useState)("");
  const [showManualCode, setShowManualCode] = (0, import_react3.useState)(false);
  const refreshData = async () => {
    try {
      const [st, sc, lg] = await Promise.all([
        apiGetStatus(),
        apiScanSaves(),
        apiGetRecentLogs()
      ]);
      if (st.success) {
        setStatus(st);
        if (st.is_authenticated) {
          setAuthUrl("");
          setLoggingIn(false);
        }
      }
      if (sc.success) {
        setEmulators(sc.emulators);
        setTotalSaves(sc.total_saves);
      }
      if (lg.success) {
        setLogs(lg.logs);
      }
    } catch (e) {
      console.error("[syncMyShit] Failed to refresh data:", e);
    }
  };
  (0, import_react3.useEffect)(() => {
    refreshData();
  }, []);
  (0, import_react3.useEffect)(() => {
    if (!loggingIn) return;
    const interval = setInterval(async () => {
      try {
        const st = await apiGetStatus();
        if (st.success && st.is_authenticated) {
          setStatus(st);
          setLoggingIn(false);
          setAuthUrl("");
          toaster.toast({
            title: "Google Drive Connected!",
            body: `Signed in as ${st.email}`,
            duration: 4500
          });
          await refreshData();
        }
      } catch (e) {
      }
    }, 2e3);
    return () => clearInterval(interval);
  }, [loggingIn]);
  const handleStartGoogleLogin = async () => {
    setLoggingIn(true);
    try {
      const res = await apiStartGoogleLogin();
      if (res.success && res.auth_url) {
        setAuthUrl(res.auth_url);
        toaster.toast({
          title: "Google Sign-In Started",
          body: "Complete sign-in in the opened browser window.",
          duration: 5e3
        });
      } else {
        setLoggingIn(false);
        toaster.toast({
          title: "Sign-In Error",
          body: res.error || "Failed to start Google authentication",
          duration: 5e3
        });
      }
    } catch (err) {
      setLoggingIn(false);
      toaster.toast({
        title: "Sign-In Error",
        body: String(err?.message || err),
        duration: 5e3
      });
    }
  };
  const handleSubmitManualCode = async () => {
    if (!manualCode.trim()) return;
    try {
      const res = await apiSubmitAuthCode(manualCode.trim());
      if (res.success) {
        setManualCode("");
        setShowManualCode(false);
        setLoggingIn(false);
        setAuthUrl("");
        toaster.toast({
          title: "Google Drive Connected!",
          body: `Signed in as ${res.email || "Google Drive User"}`,
          duration: 4500
        });
        await refreshData();
      } else {
        toaster.toast({
          title: "Authorization Error",
          body: res.error || "Invalid code or failed to exchange token",
          duration: 5e3
        });
      }
    } catch (err) {
      toaster.toast({
        title: "Authorization Error",
        body: String(err?.message || err),
        duration: 5e3
      });
    }
  };
  const handleSignOut = async () => {
    try {
      await apiSignOutGoogle();
      toaster.toast({
        title: "Google Drive Disconnected",
        body: "You have signed out of Google Drive.",
        duration: 3500
      });
      await refreshData();
    } catch (err) {
      toaster.toast({
        title: "Sign-Out Error",
        body: String(err?.message || err),
        duration: 4e3
      });
    }
  };
  const handleFullSync = async () => {
    if (!status?.is_authenticated) {
      toaster.toast({
        title: "Google Drive Required",
        body: "Please connect Google Drive first.",
        duration: 4e3
      });
      return;
    }
    setSyncing(true);
    setSyncTargetId("all");
    try {
      const res = await apiRunSync();
      toaster.toast({
        title: "Google Drive Sync",
        body: res.message || "Save synchronization complete!",
        duration: 4e3
      });
      await refreshData();
    } catch (err) {
      toaster.toast({
        title: "Sync Error",
        body: String(err?.message || err),
        duration: 5e3
      });
    } finally {
      setSyncing(false);
      setSyncTargetId(null);
    }
  };
  const handleSingleSync = async (emu) => {
    if (!status?.is_authenticated) {
      toaster.toast({
        title: "Google Drive Required",
        body: "Please connect Google Drive first.",
        duration: 4e3
      });
      return;
    }
    setSyncing(true);
    setSyncTargetId(emu.id);
    try {
      const res = await apiRunSync(emu.id);
      toaster.toast({
        title: `Drive Sync: ${emu.name}`,
        body: res.message || `Synchronized ${emu.name} with Google Drive`,
        duration: 3500
      });
      await refreshData();
    } catch (err) {
      toaster.toast({
        title: `Error: ${emu.name}`,
        body: String(err?.message || err),
        duration: 5e3
      });
    } finally {
      setSyncing(false);
      setSyncTargetId(null);
    }
  };
  const handleToggleWatcher = async (enabled) => {
    try {
      const res = await apiToggleWatcher(enabled);
      if (res.success && status) {
        setStatus({ ...status, auto_sync: res.auto_sync });
        toaster.toast({
          title: "Auto-Sync Watcher",
          body: enabled ? "Auto-sync enabled (syncs to Drive on game exit)" : "Auto-sync paused",
          duration: 3e3
        });
      }
    } catch (e) {
      toaster.toast({
        title: "Watcher Error",
        body: String(e),
        duration: 4e3
      });
    }
  };
  const handleClearLogs = async () => {
    try {
      await apiClearLogs();
      setLogs([]);
      toaster.toast({
        title: "Activity Log",
        body: "Cleared recent activity",
        duration: 2500
      });
    } catch (e) {
      console.error(e);
    }
  };
  const handleUpdatePlugin = async () => {
    setUpdating(true);
    try {
      const res = await apiUpdatePlugin();
      if (res.success) {
        toaster.toast({
          title: "syncMyShit Updated!",
          body: res.message || "Updated to latest version! Reopen QAM to apply.",
          duration: 6e3
        });
        await refreshData();
      } else {
        toaster.toast({
          title: "Update Failed",
          body: res.error || "Could not complete update.",
          duration: 5e3
        });
      }
    } catch (err) {
      toaster.toast({
        title: "Update Error",
        body: String(err?.message || err),
        duration: 5e3
      });
    } finally {
      setUpdating(false);
    }
  };
  return /* @__PURE__ */ window.SP_REACT.createElement(
    "div",
    {
      style: {
        width: "100%",
        maxWidth: "100%",
        boxSizing: "border-box",
        overflowX: "hidden",
        padding: "0 2px"
      }
    },
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Google Drive Cloud" }, status?.is_authenticated ? /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          display: "flex",
          flexDirection: "column",
          width: "100%",
          maxWidth: "100%",
          boxSizing: "border-box",
          background: "rgba(34, 197, 94, 0.1)",
          border: "1px solid rgba(34, 197, 94, 0.3)",
          borderRadius: "6px",
          padding: "8px 10px",
          gap: "4px"
        }
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaCheckCircle, { style: { color: "#22c55e" }, size: 13 }), /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { fontWeight: 700, fontSize: "13px", color: "#22c55e" } }, "Connected to Google Drive")),
      /* @__PURE__ */ window.SP_REACT.createElement(
        "div",
        {
          style: {
            fontSize: "12px",
            color: "#f8fafc",
            fontWeight: 600,
            overflowWrap: "anywhere",
            wordBreak: "break-all",
            marginTop: "2px"
          }
        },
        status.email || "Google Account"
      ),
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: "11px", color: "#94a3b8" } }, "Drive Folder: ", /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: "#38bdf8", fontWeight: 600 } }, "syncMyShit/"))
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleSignOut
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSignOutAlt, { size: 12 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, "Disconnect Google Account"))
    ))) : /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          display: "flex",
          flexDirection: "column",
          width: "100%",
          maxWidth: "100%",
          boxSizing: "border-box",
          background: "rgba(239, 68, 68, 0.1)",
          border: "1px solid rgba(239, 68, 68, 0.3)",
          borderRadius: "6px",
          padding: "8px 10px",
          gap: "4px"
        }
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaExclamationCircle, { style: { color: "#ef4444" }, size: 13 }), /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { fontWeight: 700, fontSize: "13px", color: "#ef4444" } }, "Not Connected")),
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: "11px", color: "#94a3b8", lineHeight: 1.35 } }, "Sign in with your Google account to sync saves across your Steam Deck and Android handhelds.")
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleStartGoogleLogin,
        disabled: loggingIn
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaGoogle, { size: 13 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, loggingIn ? "Waiting for Sign-In..." : "Sign In to Google Drive"))
    )), loggingIn && authUrl && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          fontSize: "11px",
          color: "#94a3b8",
          lineHeight: 1.35,
          width: "100%",
          boxSizing: "border-box",
          background: "rgba(0, 0, 0, 0.25)",
          padding: "6px 8px",
          borderRadius: "4px"
        }
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { color: "#38bdf8", fontWeight: 600, marginBottom: "2px" } }, "Authorization In Progress"),
      "Complete the sign-in prompt in your browser window. Once approved, syncMyShit will connect automatically."
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: () => setShowManualCode(!showManualCode)
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaKey, { size: 11 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, showManualCode ? "Hide Manual Input" : "Paste Authorization Code Manually"))
    )), showManualCode && /* @__PURE__ */ window.SP_REACT.createElement(window.SP_REACT.Fragment, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.TextField,
      {
        label: "Authorization Code / URL",
        value: manualCode,
        onChange: (e) => setManualCode(e.target.value)
      }
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleSubmitManualCode,
        disabled: !manualCode.trim()
      },
      "Submit Code"
    ))))),
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Quick Sync" }, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleFullSync,
        disabled: syncing || !status?.is_authenticated
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "8px", width: "100%" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSyncAlt, { className: syncing && syncTargetId === "all" ? "fa-spin" : "" }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, syncing && syncTargetId === "all" ? "Syncing to Drive..." : !status?.is_authenticated ? "Sign in to Sync Saves" : "\u26A1 Sync All Saves Now"))
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.Field,
      {
        label: "Detected Saves",
        description: `${totalSaves} saves (${emulators.length} emulators)`
      },
      /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: "#22c55e", fontWeight: 700, fontSize: "12px" } }, "Ready")
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.Field,
      {
        label: "Last Drive Sync",
        description: formatTimestamp(status?.last_sync_timestamp || 0)
      },
      /* @__PURE__ */ window.SP_REACT.createElement(FaCheckCircle, { style: { color: status?.last_sync_timestamp ? "#38bdf8" : "#64748b" } })
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ToggleField,
      {
        label: "Auto-Sync on Game Exit",
        description: "Uploads saves to Google Drive when emulator closes",
        checked: status?.auto_sync ?? true,
        onChange: handleToggleWatcher
      }
    ))),
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: `Emulators (${emulators.length})` }, emulators.length === 0 ? /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: "12px", color: "#94a3b8", padding: "4px 0", textAlign: "center", width: "100%" } }, "No emulator save directories found. Check your EmuDeck or emulator installation.")) : emulators.map((emu) => {
      const isThisSyncing = syncing && syncTargetId === emu.id;
      return /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, { key: emu.id }, /* @__PURE__ */ window.SP_REACT.createElement(
        "div",
        {
          style: {
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            width: "100%",
            maxWidth: "100%",
            gap: "8px",
            minWidth: 0,
            boxSizing: "border-box"
          }
        },
        /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { flex: "1 1 auto", minWidth: 0, overflow: "hidden" } }, /* @__PURE__ */ window.SP_REACT.createElement(
          "div",
          {
            style: {
              fontWeight: 600,
              fontSize: "13px",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis"
            },
            title: emu.name
          },
          emu.name
        ), /* @__PURE__ */ window.SP_REACT.createElement(
          "div",
          {
            style: {
              fontSize: "11px",
              color: "#94a3b8",
              whiteSpace: "nowrap",
              overflow: "hidden",
              textOverflow: "ellipsis"
            }
          },
          emu.category,
          " \u2022 ",
          emu.save_count,
          " save",
          emu.save_count === 1 ? "" : "s"
        )),
        /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { flex: "0 0 auto" } }, /* @__PURE__ */ window.SP_REACT.createElement(
          import_ui.ButtonItem,
          {
            layout: "inline",
            onClick: () => handleSingleSync(emu),
            disabled: syncing || !status?.is_authenticated
          },
          /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", gap: "5px", fontSize: "12px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaSyncAlt, { className: isThisSyncing ? "fa-spin" : "", size: 11 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, isThisSyncing ? "..." : "Sync"))
        ))
      ));
    })),
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Recent Activity" }, logs.length === 0 ? /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { fontSize: "12px", color: "#94a3b8", padding: "4px 0", textAlign: "center", width: "100%" } }, "No recent sync activity yet.")) : logs.slice(0, 8).map((entry, idx) => /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, { key: idx }, /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          display: "flex",
          flexDirection: "column",
          width: "100%",
          maxWidth: "100%",
          boxSizing: "border-box",
          gap: "2px",
          fontSize: "11px",
          minWidth: 0,
          background: "rgba(255, 255, 255, 0.03)",
          padding: "5px 8px",
          borderRadius: "4px",
          borderLeft: `3px solid ${entry.type === "upload" ? "#4ade80" : "#38bdf8"}`
        }
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", justifyContent: "space-between", alignItems: "center", width: "100%", minWidth: 0 } }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: entry.type === "upload" ? "#4ade80" : "#38bdf8", fontWeight: 700, fontSize: "11px" } }, "[", entry.type.toUpperCase(), "] ", entry.emulator), /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: "#64748b", fontSize: "10px", whiteSpace: "nowrap", flexShrink: 0, marginLeft: "6px" } }, entry.time)),
      /* @__PURE__ */ window.SP_REACT.createElement(
        "div",
        {
          style: {
            color: "#cbd5e1",
            fontSize: "11px",
            overflowWrap: "anywhere",
            wordBreak: "break-word",
            lineHeight: 1.3,
            marginTop: "2px"
          }
        },
        entry.message
      )
    ))), logs.length > 0 && /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleClearLogs
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaTrashAlt, { size: 11 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, "Clear Activity Log"))
    ))),
    /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSection, { title: "Plugin Management" }, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(import_ui.Field, { label: "Version", description: "syncMyShit Decky Plugin" }, /* @__PURE__ */ window.SP_REACT.createElement("span", { style: { color: "#38bdf8", fontWeight: 700, fontSize: "12px" } }, "v1.0.15"))), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      import_ui.ButtonItem,
      {
        layout: "below",
        onClick: handleUpdatePlugin,
        disabled: updating
      },
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { display: "flex", alignItems: "center", justifyContent: "center", gap: "6px" } }, /* @__PURE__ */ window.SP_REACT.createElement(FaArrowAltCircleUp, { className: updating ? "fa-spin" : "", size: 13 }), /* @__PURE__ */ window.SP_REACT.createElement("span", null, updating ? "Updating Plugin..." : "\u26A1 Update Plugin to Latest"))
    )), /* @__PURE__ */ window.SP_REACT.createElement(import_ui.PanelSectionRow, null, /* @__PURE__ */ window.SP_REACT.createElement(
      "div",
      {
        style: {
          fontSize: "10px",
          color: "#64748b",
          lineHeight: 1.4,
          width: "100%",
          boxSizing: "border-box",
          padding: "4px 0"
        }
      },
      "Commands in Desktop Mode (Konsole):",
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" } }, "curl -sSL .../update-decky.sh | bash"),
      /* @__PURE__ */ window.SP_REACT.createElement("div", { style: { color: "#94a3b8", fontFamily: "monospace", marginTop: "2px", overflowWrap: "anywhere", wordBreak: "break-all" } }, "curl -sSL .../uninstall-decky.sh | bash")
    )))
  );
};
var index_default = definePlugin(() => {
  return {
    name: "syncMyShit",
    titleView: /* @__PURE__ */ window.SP_REACT.createElement("div", { className: import_ui.staticClasses.Title }, "syncMyShit"),
    content: /* @__PURE__ */ window.SP_REACT.createElement(Content, null),
    icon: /* @__PURE__ */ window.SP_REACT.createElement(FaGamepad, null),
    onDismount() {
    }
  };
});
export {
  index_default as default
};
