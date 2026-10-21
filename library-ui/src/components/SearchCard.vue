<template>
  <section class="search-card">
    <slot />
  </section>
</template>

<style scoped>
.search-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 16px 18px;
  margin-bottom: 16px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
}
</style>
