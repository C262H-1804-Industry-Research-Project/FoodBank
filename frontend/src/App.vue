<template>
  <router-view />
</template>

<style>
/* 这里不写 scoped，保持全局基础重置生效 */
html, body, #app {
  margin: 0;
  padding: 0;
  min-height: 100vh;
}
</style>