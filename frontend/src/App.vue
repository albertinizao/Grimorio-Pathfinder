<script setup lang="ts">
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";
</script>

<template>
  <main class="app-shell">
    <section class="card">
      <p class="eyebrow">Grimorio Pathfinder</p>
      <h1>Arquitectura preparada</h1>
      <p>
        Esta SPA Vue vive separada del backend y solo consumirá la API local.
        No lee SQLite ni archivos de datos directamente.
      </p>

      <dl class="grid">
        <div>
          <dt>Backend local</dt>
          <dd>{{ apiBaseUrl }}</dd>
        </div>
        <div>
          <dt>Modo</dt>
          <dd>Offline-first</dd>
        </div>
        <div>
          <dt>Stack</dt>
          <dd>Vue 3 + Vite + TypeScript</dd>
        </div>
        <div>
          <dt>Objetivo</dt>
          <dd>Tablet-friendly dark UI</dd>
        </div>
      </dl>
    </section>
  </main>
</template>
