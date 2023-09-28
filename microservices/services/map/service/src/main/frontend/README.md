# Vue 3 + TypeScript + Vite

This template should help get you started developing with Vue 3 and TypeScript in Vite. The template uses Vue 3 `<script setup>` SFCs, check out the [script setup docs](https://v3.vuejs.org/api/sfc-script-setup.html#sfc-script-setup) to learn more.

## Recommended IDE Setup

- [VS Code](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur) + [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin).

## Type Support For `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [TypeScript Vue Plugin (Volar)](https://marketplace.visualstudio.com/items?itemName=Vue.vscode-typescript-vue-plugin) to make the TypeScript language service aware of `.vue` types.

If the standalone TypeScript plugin doesn't feel fast enough to you, Volar has also implemented a [Take Over Mode](https://github.com/johnsoncodehk/volar/discussions/471#discussioncomment-1361669) that is more performant. You can enable it by the following steps:

1. Disable the built-in TypeScript Extension
   1. Run `Extensions: Show Built-in Extensions` from VSCode's command palette
   2. Find `TypeScript and JavaScript Language Features`, right click and select `Disable (Workspace)`
2. Reload the VSCode window by running `Developer: Reload Window` from the command palette.


# Noob Notes

# NVM & NPM Setup

NVM (Node Version Manager) is a utility that can be used to manage node.js versions on your machine.

To install, run the following command:
`curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash`

After installing nvm, open a new terminal, or source bash so you can run nvm commands.

To install node (and NPM) run the following command:
`nvm install v16.20.2`

After that has finished, run the following command `npm version`, and verify that you get the following output
```json
{
  npm: '8.19.4',
  node: '16.20.2',
  v8: '9.4.146.26-node.26',
  uv: '1.43.0',
  zlib: '1.2.11',
  brotli: '1.0.9',
  ares: '1.19.1',
  modules: '93',
  nghttp2: '1.47.0',
  napi: '8',
  llhttp: '6.0.11',
  openssl: '1.1.1v+quic',
  cldr: '41.0',
  icu: '71.1',
  tz: '2022f',
  unicode: '14.0',
  ngtcp2: '0.8.1',
  nghttp3: '0.7.0'
}
```

# NPM

## Essential commands

`npm init` initializes a new project.  You will be prompted for things like name, version, description, etc.

`npm install <module>` is used to install node modules into your local `node_modules` folder.  Running this command without a module argument, within a project (containing a package.json) will cause all of the `dependencies` and `devDependencies` to be installed.  Adding a `--save` or `--save-dev` flag will add the installed module to the list of `dependencies` or `devDependencies` in `package.json`.

`npm run <script>` is used to run either a built-in or user-defined script defined in package.json.  This is commonly used to create a `build`, or `dev` script for building/running your project.

## package.json

package.json is analogous to pom.xml in maven.  The project `name`, `version`, `type`, `dependencies`, `devDependencies`, and `scripts` are all enumerated here.

A Quasar Project

## Install the dependencies
```bash
yarn
# or
npm install
```

### Start the app in development mode (hot-code reloading, error reporting, etc.)
```bash
quasar dev
```


### Lint the files
```bash
yarn lint
# or
npm run lint
```


### Format the files
```bash
yarn format
# or
npm run format
```



### Build the app for production
```bash
quasar build
```

### Customize the configuration
See [Configuring quasar.config.js](https://v2.quasar.dev/quasar-cli-vite/quasar-config-js).
