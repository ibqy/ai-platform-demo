<script setup>
import DefaultTheme from 'vitepress/theme'

const { Layout } = DefaultTheme
</script>

<template>
  <Layout>
    <template #home-features-before>
      <section class="sig">
        <div class="sig-card">
          <span class="sig-label">GATEWAY · 14-STEP PIPELINE</span>
          <h3 class="sig-title">一个请求穿过 AI 网关的一生</h3>
          <div class="ap-steps">
            <span class="ap-step" style="--i:1">01</span>
            <span class="ap-step" style="--i:2">02</span>
            <span class="ap-step" style="--i:3">03</span>
            <span class="ap-step" style="--i:4">04</span>
            <span class="ap-step" style="--i:5">05</span>
            <span class="ap-step" style="--i:6">06</span>
            <span class="ap-step" style="--i:7">07</span>
            <span class="ap-step" style="--i:8">08</span>
            <span class="ap-step" style="--i:9">09</span>
            <span class="ap-step" style="--i:10">10</span>
            <span class="ap-step" style="--i:11">11</span>
            <span class="ap-step" style="--i:12">12</span>
            <span class="ap-step" style="--i:13">13</span>
            <span class="ap-step" style="--i:14">14</span>
          </div>
          <p class="ap-legend">限流 → 鉴权 → 租户配额 → Prompt 检查 → 模型路由 → 缓存 → 调用 → 审计 → 计量 …（共 14 步，每步只做一件事）</p>
          <div class="ap-cards">
            <div class="ap-card"><b>智能路由</b><span>COST / PERFORMANCE / QUALITY 三种策略选模型</span></div>
            <div class="ap-card"><b>多级缓存</b><span>精确命中 → 语义命中 → 真实调用</span></div>
            <div class="ap-card"><b>虚拟线程</b><span>Java 21 承载高并发模型调用</span></div>
          </div>
          <p class="sig-note">把网关拆成 14 步责任链，每一步都可单测、可替换——这是从 0 搭一个企业级 AI 平台时，最值得先想清楚的事。</p>
        </div>
      </section>
    </template>
    <template #home-features-after>
      <div class="sig-more"><a href="https://ibqy.github.io/" target="_blank" rel="noopener">← ibqy.github.io · 探索全部项目</a></div>
    </template>
  </Layout>
</template>
