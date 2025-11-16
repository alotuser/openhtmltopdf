# JhtmlRender 开发文档

## 📖 概述

JhtmlRender 是一个轻量级的 Java 库，可将标准 HTML 标记转换为图像，并生成对应的客户端图像映射。它支持精确的 HTML 和 CSS 渲染，能够在生成的图像中保留原 HTML 中的可点击区域。

## ✨ 核心特性

- **高质量 HTML 转图像**：将 HTML 和 CSS 精准地渲染为 PNG、JPG 等格式图像
- **客户端图像映射**：保留原 HTML 中的链接并生成对应的图像映射区域
- **字体支持**：支持自定义字体目录
- **灵活配置**：可调整页面尺寸、缩放比例等参数
- **元素定位**：支持通过 CSS 选择器定位元素在图像中的位置

## 🚀 快速开始

### 添加依赖

```xml
<dependencies>
    <dependency>
        <groupId>com.github.alotuser</groupId>
        <artifactId>openhtmltopdf-jhtml</artifactId>
        <version>1.0.1</version>
    </dependency>
</dependencies>
```

### 基础使用示例

```java
// 准备 HTML 资源
String resHtml = "2.html";
String html = ResourceUtil.readUtf8Str(resHtml);
URL fonts = ResourceUtil.getResource("fonts");

// 初始化渲染器
JhtmlRender htmlRender = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
htmlRender.addFontDirectory(fonts.getPath());
htmlRender.setPageWidth(400f);
htmlRender.setPageHeight(300f);
htmlRender.setScale(1f);
htmlRender.setLoggingEnabled(true);

// 渲染为 PNG 图像
htmlRender.toPng(html, "D://" + resHtml + ".png");

System.out.println("HTML 转换完成！");
```

## 🔧 核心 API

### JhtmlRender 类

#### 配置方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `create(int imageType)` | `imageType`: 图像类型常量 | 创建渲染器实例 |
| `addFontDirectory(String path)` | `path`: 字体目录路径 | 添加字体目录 |
| `setPageWidth(float width)` | `width`: 页面宽度 | 设置渲染页面宽度 |
| `setPageHeight(float height)` | `height`: 页面高度 | 设置渲染页面高度 |
| `setScale(float scale)` | `scale`: 缩放比例 | 设置渲染缩放级别 |
| `setLoggingEnabled(boolean enabled)` | `enabled`: 布尔值 | 启用/禁用日志记录 |

#### 渲染方法

```java
// 渲染为 PNG 文件
void toPng(String html, String outputPath)

// 渲染为 BufferedImage
BufferedImage toImage(String html, BuilderConfig config)

// 带自定义配置的渲染
htmlRender.toImage(html, builder -> {
    builder.useFont(new File("myfont"), "myfont");
});
```

### AsJsoupProcessor 类

#### 元素定位方法

```java
// 按 CSS 类选择元素
Map<Element, Rectangle> getElementsByClass(String className)

// 使用 CSS 选择器
Map<Element, Rectangle> select(String cssSelector)

// 使用 XPath 选择
Map<Element, Rectangle> selectXpath(String xpath)
```

## 🗺️ 生成客户端图像映射

以下是实现客户端图像映射的完整示例：

```java
public class HtmlToImageWithMap {
    public static void main(String[] args) throws Exception {
        String resHtml = "2.html";
        String html = ResourceUtil.readUtf8Str(resHtml);
        
        // 初始化渲染器
        JhtmlRender htmlRender = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
        htmlRender.setPageWidth(800f);
        htmlRender.setPageHeight(600f);
        
        // 启用 Jsoup 处理器
        AsJsoupProcessor ajp = htmlRender.useJsoup();
        
        // 渲染图像
        htmlRender.toPng(html, "D://" + resHtml + ".png");
        
        // 获取所有链接元素
        Map<Element, Rectangle> linkElements = ajp.select("a[href]");
        
        // 生成图像映射 HTML
        String imageMap = generateImageMap(linkElements, "generated-image");
        
        // 输出包含图像映射的 HTML
        String outputHtml = "<html><body>\n" +
            "<img src='" + resHtml + ".png' usemap='#" + "generated-image" + "' alt='Rendered HTML'>\n" +
            imageMap +
            "</body></html>";
            
        FileUtil.writeUtf8String(outputHtml, "D://" + resHtml + "-with-map.html");
    }
    
    private static String generateImageMap(Map<Element, Rectangle> elements, String mapName) {
        StringBuilder mapBuilder = new StringBuilder();
        mapBuilder.append("<map name='").append(mapName).append("'>\n");
        
        int areaCount = 0;
        for (Map.Entry<Element, Rectangle> entry : elements.entrySet()) {
            Element element = entry.getKey();
            Rectangle rect = entry.getValue();
            String href = element.attr("href");
            String title = element.attr("title");
            
            if (href != null && !href.isEmpty()) {
                mapBuilder.append("  <area shape='rect' coords='")
                    .append(rect.x).append(",").append(rect.y).append(",")
                    .append(rect.x + rect.width).append(",").append(rect.y + rect.height)
                    .append("' href='").append(href).append("'")
                    .append(" alt='").append(title != null ? title : "").append("'>\n");
                areaCount++;
            }
        }
        
        mapBuilder.append("</map>\n");
        System.out.println("生成 " + areaCount + " 个可点击区域");
        return mapBuilder.toString();
    }
}
```

## 🎯 元素定位与图像裁剪

```java
// 定位特定 CSS 类元素
String cssClass = "original-price";
Map<Element, Rectangle> mers = ajp.getElementsByClass(cssClass);

System.out.println("找到 " + mers.size() + " 个匹配元素");

// 裁剪第一个匹配元素
if (!mers.isEmpty()) {
    Rectangle firstRect = mers.values().stream().findFirst().get();
    
    BufferedImage original = ImageIO.read(new File("D:\\" + resHtml + ".png"));
    Rectangle rect = new Rectangle(firstRect.x, firstRect.y, firstRect.width, firstRect.height);
    
    BufferedImage cropped = ImageCropUtil.cropImage(original, rect);
    ImageIO.write(cropped, "png", new File("D:\\" + resHtml + "-cropped.png"));
    
    System.out.println("元素裁剪完成，位置: " + rect);
}
```

## 🔍 高级用法

### 自定义字体配置

```java
JhtmlRender htmlRender = JhtmlRender.create(BufferedImage.TYPE_INT_ARGB);

// 添加多个字体目录
htmlRender.addFontDirectory("src/main/resources/fonts");
htmlRender.addFontDirectory("/system/fonts");

// 或者使用自定义字体文件
htmlRender.toImage(html, builder -> {
    builder.useFont(new File("path/to/custom-font.ttf"), "MyCustomFont");
});
```

### 批量处理

```java
public class BatchHtmlRenderer {
    public void processMultipleFiles(List<String> htmlFiles) {
        JhtmlRender render = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
        render.setPageWidth(1024f);
        render.setPageHeight(768f);
        
        AsJsoupProcessor processor = render.useJsoup();
        
        for (String htmlFile : htmlFiles) {
            try {
                String html = ResourceUtil.readUtf8Str(htmlFile);
                String outputImage = "D:/output/" + htmlFile + ".png";
                
                render.toPng(html, outputImage);
                
                // 为每个文件生成图像映射
                Map<Element, Rectangle> links = processor.select("a[href]");
                generateImageMap(links, htmlFile + "-map");
                
            } catch (Exception e) {
                System.err.println("处理文件失败: " + htmlFile + ", 错误: " + e.getMessage());
            }
        }
    }
}
```

## 📋 最佳实践

### 1. 资源管理

```java
// 使用 try-with-resources 确保资源释放
try (JhtmlRender render = JhtmlRender.create(BufferedImage.TYPE_INT_RGB)) {
    render.toPng(html, outputPath);
} catch (Exception e) {
    // 异常处理
}
```

### 2. 错误处理

```java
public class SafeHtmlRenderer {
    public boolean renderHtmlToImage(String html, String outputPath) {
        try {
            JhtmlRender render = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
            render.setLoggingEnabled(true);
            
            render.toPng(html, outputPath);
            return true;
            
        } catch (RenderException e) {
            System.err.println("渲染失败: " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("IO 错误: " + e.getMessage());
            return false;
        }
    }
}
```

### 3. 性能优化

```java
// 复用渲染器实例
public class HtmlRenderService {
    private final JhtmlRender render;
    
    public HtmlRenderService() {
        this.render = JhtmlRender.create(BufferedImage.TYPE_INT_RGB);
        this.render.addFontDirectory("common/fonts");
    }
    
    public void renderToImage(String html, String outputPath) {
        render.toPng(html, outputPath);
    }
}
```

## 🐛 故障排除

### 常见问题

**问题：渲染结果空白**
- 检查 HTML 内容是否有效
- 验证字体路径是否正确
- 确认页面尺寸是否合适

**问题：元素定位不准确**
- 检查 CSS 选择器语法
- 确认元素在渲染时可见
- 验证页面缩放设置

**问题：图像映射坐标错误**
- 确保在渲染后立即获取元素位置
- 检查页面是否完全加载

## 📚 应用场景

1. **邮件模板渲染**：确保在不同邮件客户端中显示一致
2. **报告生成**：将动态 HTML 报告转换为可分享的图像
3. **网页快照**：创建网页的可视化存档
4. **内容保护**：将文本内容渲染为图像防止爬取

这个库在需要将 HTML 内容以图像形式分享或存档，同时保留交互功能的场景下特别有用。希望这份文档能帮助你更好地使用 JhtmlRender！
