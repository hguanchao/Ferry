import { theme as antdTheme, type ThemeConfig } from 'antd'

/**
 * 树密度：侧栏窄列树与 TreeSelect 下拉（上级菜单 / 上级部门）必须共用同一组值。
 * 两者是独立的组件级 token，只配 Tree 的话下拉仍会沿用 controlHeightSM 的
 * 28px 行高 / 28px 缩进，看起来比侧栏松一大截。
 */
const TREE_TITLE_HEIGHT = 26
const TREE_INDENT_SIZE = 16

/**
 * Ant Design 主题
 *
 * 只覆盖企业后台真正会被感知的令牌：主色、圆角、控件高度、表格密度、菜单项。
 * 其余交给默认算法，避免为了「定制感」堆砌无效变量。
 */
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
        /**
         * 树密度。
         *
         * antd 的默认缩进是 28px/级、行高随 controlHeightSM 一起放大到 34px 行节奏，
         * 而渡口的树大多长在 248px 的侧栏窄列里（组织架构 / 行政区划 / 字典），
         * 四级以后文字就会被挤成两行。这里把行高与缩进一起收窄。
         *
         * indentSize 必须显式写：组件级覆盖 titleHeight 不会回写到它，
         * 不写会停留在默认的 28px。两个值需成对调整，缩进才真的比行高更紧凑。
         */
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
      Tooltip: {
        colorBgSpotlight: dark ? '#1b2537' : 'rgba(16, 24, 40, 0.92)',
      },
      Descriptions: {
        labelBg: dark ? '#172032' : '#f8fafc',
      },
      Popover: {
        titleMinWidth: 120,
      },
    },
  }
}
