import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'AI 平台中台实战',
  description: '纯 Java 从零实现企业级 AI 网关中台：模型路由、多租户、多级缓存、安全过滤与可观测计量',
  base: '/ai-platform-demo/',
  lastUpdated: true,
  markdown: {
    config(md) {
      const defaultLink =
        md.renderer.rules.link_open ||
        ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))
      md.renderer.rules.link_open = (tokens, idx, options, env, self) => {
        const href = tokens[idx].attrGet('href')
        if (href && href.startsWith('../')) {
          const rel = href.replace(/^(\.\.\/)+/, '')
          const kind = /\.[A-Za-z]+$/.test(rel) ? 'blob' : 'tree'
          tokens[idx].attrSet('href', `https://github.com/ibqy/ai-platform-demo/${kind}/main/${rel}`)
        }
        return defaultLink(tokens, idx, options, env, self)
      }
    }
  },
  themeConfig: {
    nav: [
      { text: '首页', link: '/' },
      { text: 'GitHub', link: 'https://github.com/ibqy/ai-platform-demo' }
    ],
    sidebar: [
      {
        text: '学习笔记',
        items: [
          { text: '01 · 模型管理', link: '/01-模型管理' },
          { text: '02 · 多租户架构', link: '/02-多租户架构' },
          { text: '03 · AI 网关', link: '/03-AI网关' },
          { text: '04 · 多级缓存', link: '/04-多级缓存' },
          { text: '05 · 异步任务', link: '/05-异步任务' },
          { text: '06 · Prompt 平台', link: '/06-Prompt平台' },
          { text: '07 · 安全层', link: '/07-安全层' },
          { text: '08 · 可观测计量', link: '/08-可观测计量' },
          { text: '09 · 面试踩坑总结', link: '/09-面试踩坑总结' }
        ]
      }
    ],
    socialLinks: [
      { icon: 'github', link: 'https://github.com/ibqy/ai-platform-demo' }
    ],
    search: { provider: 'local' },
    outline: { level: [2, 3], label: '本页目录' },
    docFooter: { prev: '上一篇', next: '下一篇' },
    lastUpdated: { text: '最后更新于' },
    darkModeSwitchLabel: '外观',
    lightModeSwitchTitle: '切换到浅色模式',
    darkModeSwitchTitle: '切换到深色模式',
    sidebarMenuLabel: '文档',
    returnToTopLabel: '回到顶部',
    footer: {
      message: '个人教学项目 · 代码可跑 · 注释记录设计取舍',
      copyright: 'Copyright © 2026 ibqy'
    }
  }
})
