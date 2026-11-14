import { theme as antdTheme, type ThemeConfig } from 'antd'

/** 树密度常量：侧栏窄列树与 TreeSelect 下拉必须共用同一组值 */
const TREE_TITLE_HEIGHT = 26
const TREE_INDENT_SIZE = 16

/** Ant Design 主题：只覆盖真正会被感知的令牌，其余交给默认算法 */
export function buildTheme(mode: 'light' | 'dark'): ThemeConfig {
  const dark = mode === 'dark'

  return {
    algorithm: dark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      colorPrimary: '#1677ff',
      colorSuccess: '#16a34a',
      colorWarning: '#d97706',
      colorError: '#dc2626',
      colorInfo: '#1677ff',

      colorTextBase: dark ? '#eef2f8' : '#101828',
      colorBgBase: dark ? '#0b1220' : '#ffffff',
      colorBorder: dark ? '#2d3a51' : '#d8dfe9',
      colorBorderSecondary: dark ? '#232e42' : '#eef1f6',

      borderRadius: 8,
      borderRadiusLG: 12,
      borderRadiusSM: 6,
      borderRadiusXS: 4,

      controlHeight: 36,
      controlHeightSM: 28,
      controlHeightLG: 40,

      fontSize: 14,
      fontFamily:
        "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif",
      lineHeight: 1.5715,

      wireframe: false,
      motionDurationMid: '0.18s',
    },
    components: {
      Button: {
        fontWeight: 500,
        primaryShadow: 'none',
        defaultShadow: 'none',
        dangerShadow: 'none',
        paddingInline: 16,
      },
      Table: {
        headerBg: dark ? '#172032' : '#f8fafc',
        headerColor: dark ? '#b6c0d0' : '#475467',
        headerSplitColor: 'transparent',
        borderColor: dark ? '#232e42' : '#eef1f6',
        rowHoverBg: dark ? '#172032' : '#f8fbff',
        cellPaddingBlock: 12,
        cellPaddingInline: 14,
        headerBorderRadius: 0,
        /** 表体字号比页面基准小一号，密排时一屏能多放两行 */
        cellFontSize: 13,
      },
      Menu: {
        itemBg: 'transparent',
        subMenuItemBg: 'transparent',
        itemHoverBg: 'transparent',
        itemSelectedBg: 'transparent',
        itemColor: dark ? '#b6c0d0' : '#475467',
        itemSelectedColor: dark ? '#7cb3ff' : '#0b4fc4',
        itemHeight: 38,
        itemBorderRadius: 8,
        itemMarginInline: 0,
        itemMarginBlock: 2,
        iconSize: 15,
        collapsedIconSize: 17,
      },
      Input: {
        activeShadow: '0 0 0 3px rgba(22, 119, 255, 0.12)',
        hoverBorderColor: '#4096ff',
        paddingBlock: 6,
      },
      Select: {
        optionSelectedBg: dark ? 'rgba(22, 119, 255, 0.16)' : '#e6f4ff',
        optionSelectedColor: dark ? '#7cb3ff' : '#0b4fc4',
        optionSelectedFontWeight: 550,
      },
      Modal: {
        titleFontSize: 16,
        contentBg: dark ? '#121a2a' : '#ffffff',
        headerBg: 'transparent',
      },
      Drawer: {
        colorBgElevated: dark ? '#121a2a' : '#ffffff',
      },
      Tabs: {
        itemSelectedColor: dark ? '#7cb3ff' : '#0b4fc4',
        inkBarColor: '#1677ff',
        horizontalItemPadding: '10px 0',
        titleFontSize: 13,
      },
      Tree: {
        nodeSelectedBg: dark ? 'rgba(22, 119, 255, 0.16)' : '#e6f4ff',
        /** 树密度：antd 默认行高缩进在 248px 侧栏窄列里放不下，行高与缩进成对收窄，indentSize 必须显式写 */
        titleHeight: TREE_TITLE_HEIGHT,
        indentSize: TREE_INDENT_SIZE,
      },
      TreeSelect: {
        titleHeight: TREE_TITLE_HEIGHT,
        indentSize: TREE_INDENT_SIZE,
      },
      Tag: {
        defaultBg: dark ? '#172032' : '#f8fafc',
        defaultColor: dark ? '#b6c0d0' : '#475467',
      },
      Form: {
        /** 标签是辅助信息，比正文小一号、颜色降一级 */
        labelColor: dark ? '#b6c0d0' : '#475467',
        labelFontSize: 13,
      },
      Tooltip: {
        colorBgSpotlight: dark ? '#1b2537' : 'rgba(16, 24, 40, 0.92)',
      },
      Descriptions: {
        labelBg: dark ? '#172032' : '#f8fafc',
        labelColor: dark ? '#8593a8' : '#98a2b3',
      },
      Popover: {
        titleMinWidth: 120,
      },
      Pagination: {
        /** 分页项高度收窄到与表格行接近，页码退成页脚的一部分，不抢主体内容 */
        itemSize: 28,
        itemSizeSM: 24,
      },
    },
  }
}
