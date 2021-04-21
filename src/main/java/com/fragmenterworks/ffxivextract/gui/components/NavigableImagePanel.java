package com.fragmenterworks.ffxivextract.gui.components;

import com.fragmenterworks.ffxivextract.helpers.Utils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * <code>NavigableImagePanel</code>は、高品質の表示と満足のいくパフォーマンスを実現する適応レンダリングを使用して、
 * 簡単かつ簡単にズームインおよびズームアウトおよびパンできる画像を表示する軽量コンテナです。
 * <p>
 * <h3>Image</h3>
 * <p>画像はコンストラクターを介して読み込まれます:</p>
 * <pre>
 * NavigableImagePanel panel = new NavigableImagePanel(image);
 * </pre>
 * またはセッターを使用:
 * <pre>
 * NavigableImagePanel panel = new NavigableImagePanel();
 * panel.setImage(image);
 * </pre>
 * 画像を設定すると、最初はコンポーネントの中央にペイントされ、アスペクト比が維持された状態で、
 * 可能な限り最大のサイズで完全に表示されます。 これは画像サイズの100％として定義され、対応するズームレベルは1.0です。
 * </p>
 * <h3>Zooming</h3>
 * <p>
 * ズームは、マウスのスクロールホイール（デフォルト）またはマウスの2つのボタンを使用して * インタラクティブに制御することも、
 * プログラムで制御して、プログラマーが他のカスタムズーム方法を実装できるようにすることもできます。
 * マウスにスクロールホイールがない場合は、ズームデバイスをマウスボタンに設定します:
 * <pre>
 * panel.setZoomDevice(ZoomDevice.MOUSE_BUTTON);
 * </pre>
 * マウスの左ボタンは、ズームインモードとズームアウトモードを切り替えるトグルスイッチとして機能し、
 * 右ボタンは画像を1つずつズームします（デフォルトは20％）。 ズームの増分値は、次の方法で変更できます:
 * <pre>
 * panel.setZoomIncrement(newZoomIncrement);
 * </pre>
 * プログラムによるズーム制御を提供する場合は、ズームデバイスをnoneに設定して、
 * ズームの目的でマウスホイールとボタンの両方を無効にします:
 * <pre>
 * panel.setZoomDevice(ZoomDevice.NONE);
 * </pre>
 * <code>setZoom()</code>を使用してズームレベルを変更します
 * </p>
 * <p>
 * ズームは常にマウスポインタが現在あるポイントの周りにあるため、このポイント(ズームセンターと呼ばれます)は静止したままであり、
 * ズームインしている画像の領域が画面から消えないようにします。 ズームセンターは画面上の同じ位置に留まり、
 * 他のすべてのポイントは半径方向に離れる方向（ズームイン時）またはそれに向かって移動します（ズームアウト時）。
 * プログラムで制御されるズームの場合、ズーム中心は<code>setZoom()</code>が呼び出されたときに指定されます。:
 * <pre>
 * panel.setZoom(newZoomLevel, newZoomingCenter);
 * </pre>
 * または、ズームの中心が指定されていない場合は、パネルの中心に最も近い画像のポイントと見なされます:
 * <pre>
 * panel.setZoom(newZoomLevel);
 * </pre>
 * </p>
 * <p>
 * ズームレベルの下限や上限はありません。
 * </p>
 * <h3>Navigation</h3>
 * <p><code>NavigableImagePanel</code>は、ナビゲーションにスクロールバーを使用しませんが、
 * パネルの左上隅にあるナビゲーション画像に依存します。ナビゲーション画像は、パネルに表示される画像の小さな複製です。
 * ナビゲーション画像の任意のポイントをクリックすると、画像のその部分は、中央に配置されてパネルに表示されます。
 * ナビゲーション画像もメイン画像と同じようにズームできます。
 * パネル内の画像の位置を調整するには、左ボタンを使用してマウスで画像をドラッグします。
 * プログラムによる画像ナビゲーションの場合、ナビゲーション画像を無効にします:
 * <pre>
 * panel.setNavigationImageEnabled(false)
 * </pre>
 * <code>getImageOrigin()</code>と<code>setImageOrigin()</code>を使用して、パネル内で画像を移動します。</p>
 * <h3>Rendering</h3>
 * <p><code>NavigableImagePanel</code>は、画像レンダリングに最近隣内挿法を使用します(Javaのデフォルト)。
 * 拡大縮小された画像が元の画像よりも大きくなると、双一次補間が適用されますが、
 * パネルに表示されている画像の部分にのみ適用されます。 この補間変更しきい値は、
 * <code>HIGH_QUALITY_RENDERING_SCALE_THRESHOLD</code>の値を調整することで制御できます。</p>
 */
public class NavigableImagePanel extends JPanel {

    /**
     * ズームレベルの変更を識別
     */
    private static final String ZOOM_LEVEL_CHANGED_PROPERTY = "zoomLevel";

    /**
     * ズーム増分の変更を識別
     */
    private static final String ZOOM_INCREMENT_CHANGED_PROPERTY = "zoomIncrement";

    /**
     * パネル内の画像が変更されたことを識別
     */
    private static final String IMAGE_CHANGED_PROPERTY = "image";

    private static final double SCREEN_NAV_IMAGE_FACTOR = 0.15; // パネル幅の15%
    private static final double NAV_IMAGE_FACTOR = 0.3; // パネル幅の30%
    private static final double HIGH_QUALITY_RENDERING_SCALE_THRESHOLD = 1.0;
    private static final Object INTERPOLATION_TYPE =
            RenderingHints.VALUE_INTERPOLATION_BILINEAR;

    private double zoomIncrement = 0.2;
    private double zoomFactor = 1.0 + zoomIncrement;
    private double navZoomFactor = 1.0 + zoomIncrement;
    private BufferedImage image;
    private BufferedImage navigationImage;
    private int navImageWidth;
    private int navImageHeight;
    private double initialScale = 0.0;
    private double scale = 0.0;
    private double navScale = 0.0;
    private int originX = 0;
    private int originY = 0;
    private Point mousePosition;
    private Dimension previousPanelSize;
    private boolean navigationImageEnabled = true;
    private boolean highQualityRenderingEnabled = true;

    private BufferedImage bg;

    private WheelZoomDevice wheelZoomDevice = null;
    private ButtonZoomDevice buttonZoomDevice = null;

    /**
     * ズームデバイスを定義
     */
    public static class ZoomDevice {
        /**
         * パネルがズームを実装していないが、パネルを使用しているコンポーネントが
         * 実装していることを識別します（プログラムによるズーム方法）。
         */
        static final ZoomDevice NONE = new ZoomDevice("none");

        /**
         * マウスの左ボタンと右ボタンをズームデバイスとして識別
         */
        static final ZoomDevice MOUSE_BUTTON = new ZoomDevice("mouseButton");

        /**
         * マウスのスクロールホイールをズームデバイスとして識別
         */
        static final ZoomDevice MOUSE_WHEEL = new ZoomDevice("mouseWheel");

        private final String zoomDevice;

        private ZoomDevice(String zoomDevice) {
            this.zoomDevice = zoomDevice;
        }

        public String toString() {
            return zoomDevice;
        }
    }

    //このクラスは、高精度の画像座標変換に必要です。
    private static class Coords {
        double x;
        double y;

        Coords(double x, double y) {
            this.x = x;
            this.y = y;
        }

        int getIntX() {
            return (int) Math.round(x);
        }

        int getIntY() {
            return (int) Math.round(y);
        }

        public String toString() {
            return "[Coords: x=" + x + ",y=" + y + "]";
        }
    }

    private class WheelZoomDevice implements MouseWheelListener {
        @SuppressWarnings("ConstantConditions")
        public void mouseWheelMoved(MouseWheelEvent e) {
            Point p = e.getPoint();
            if (p == null)
                return;
            boolean zoomIn = (e.getWheelRotation() < 0);
            if (isInNavigationImage(p)) {
                if (zoomIn) {
                    navZoomFactor = 1.0 + zoomIncrement;
                } else {
                    navZoomFactor = 1.0 - zoomIncrement;
                }
                zoomNavigationImage();
            } else if (isInImage(p)) {
                if (zoomIn) {
                    zoomFactor = 1.0 + zoomIncrement;
                } else {
                    zoomFactor = 1.0 - zoomIncrement;
                }
                zoomImage();
            }
        }
    }

    private class ButtonZoomDevice extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            Point p = e.getPoint();
            if (SwingUtilities.isRightMouseButton(e)) {
                if (isInNavigationImage(p)) {
                    navZoomFactor = 1.0 - zoomIncrement;
                    zoomNavigationImage();
                } else if (isInImage(p)) {
                    zoomFactor = 1.0 - zoomIncrement;
                    zoomImage();
                }
            } else {
                if (isInNavigationImage(p)) {
                    navZoomFactor = 1.0 + zoomIncrement;
                    zoomNavigationImage();
                } else if (isInImage(p)) {
                    zoomFactor = 1.0 + zoomIncrement;
                    zoomImage();
                }
            }
        }
    }

    /**
     * デフォルトの画像がなく、ズームデバイスとしてマウスのスクロールホイールを使用して、
     * 新しいナビゲート可能な画像パネルを作成
     */
    public NavigableImagePanel() {
        setNavigationImageEnabled(false);
        try {
            bg = ImageIO.read(Objects.requireNonNull(getClass().getResource("/bg.png")));
        } catch (IOException e1) {
            Utils.getGlobalLogger().error(e1);
        }

        setOpaque(false);
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                if (scale > 0.0) {
                    if (isFullImageInPanel()) {
                        centerImage();
                    } else if (isImageEdgeInPanel()) {
                        scaleOrigin();
                    }
                    if (isNavigationImageEnabled()) {
                        createNavigationImage();
                    }
                    repaint();
                }
                previousPanelSize = getSize();
            }
        });

        addMouseListener(new MouseAdapter() {

            public void mouseEntered(MouseEvent e) {
                mousePosition = e.getPoint();
            }

            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (isInNavigationImage(e.getPoint())) {
                        Point p = e.getPoint();
                        displayImageAt(p);
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)
                        && !isInNavigationImage(e.getPoint())) {
                    Point p = e.getPoint();
                    moveImage(p);
                }
            }

            public void mouseMoved(MouseEvent e) {
                //ズームした後、画像のその位置が維持されるように、マウスの位置が必要
                mousePosition = e.getPoint();
            }
        });

        setZoomDevice(ZoomDevice.MOUSE_WHEEL);
    }

    /**
     * 指定された画像とズームデバイスとしてのマウススクロールホイールを使用して、新しいナビゲート可能な画像パネルを作成
     */
    @SuppressWarnings("unused")
    public NavigableImagePanel(BufferedImage image) {
        this();
        setImage(image);
    }

    private void addWheelZoomDevice() {
        if (wheelZoomDevice == null) {
            wheelZoomDevice = new WheelZoomDevice();
            addMouseWheelListener(wheelZoomDevice);
        }
    }

    private void addButtonZoomDevice() {
        if (buttonZoomDevice == null) {
            buttonZoomDevice = new ButtonZoomDevice();
            addMouseListener(buttonZoomDevice);
        }
    }

    private void removeWheelZoomDevice() {
        if (wheelZoomDevice != null) {
            removeMouseWheelListener(wheelZoomDevice);
            wheelZoomDevice = null;
        }
    }

    private void removeButtonZoomDevice() {
        if (buttonZoomDevice != null) {
            removeMouseListener(buttonZoomDevice);
            buttonZoomDevice = null;
        }
    }

    /**
     * 新しいズームデバイスを設定
     *
     * @param newZoomDevice 新しいズームデバイスのタイプを指定
     */
    @SuppressWarnings("SameParameterValue")
    private void setZoomDevice(ZoomDevice newZoomDevice) {
        if (newZoomDevice == ZoomDevice.NONE) {
            removeWheelZoomDevice();
            removeButtonZoomDevice();
        } else if (newZoomDevice == ZoomDevice.MOUSE_BUTTON) {
            removeWheelZoomDevice();
            addButtonZoomDevice();
        } else if (newZoomDevice == ZoomDevice.MOUSE_WHEEL) {
            removeButtonZoomDevice();
            addWheelZoomDevice();
        }
    }

    /**
     * 現在のズームデバイスを取得
     */
    @SuppressWarnings("unused")
    public ZoomDevice getZoomDevice() {
        if (buttonZoomDevice != null) {
            return ZoomDevice.MOUSE_BUTTON;
        } else if (wheelZoomDevice != null) {
            return ZoomDevice.MOUSE_WHEEL;
        } else {
            return ZoomDevice.NONE;
        }
    }

    /**
     * 新しい画像が設定されたときにpaintComponent()から呼び出されます
     */
    private void initializeParams() {
        double xScale = (double) getWidth() / image.getWidth();
        double yScale = (double) getHeight() / image.getHeight();
        initialScale = Math.min(xScale, yScale);
        scale = initialScale;

        //画像は最初は中央に配置されます
        centerImage();
        if (isNavigationImageEnabled()) {
            createNavigationImage();
        }
    }

    /**
     * 現在の画像をパネルの中央に配置
     */
    private void centerImage() {
        originX = (getWidth() - getScreenImageWidth()) / 2;
        originY = (getHeight() - getScreenImageHeight()) / 2;
    }

    /**
     * パネルの上部レットコーナーにナビゲーション画像を作成してレンダリングします。
     */
    private void createNavigationImage() {
        //ピクセル化効果なしでズームインできるように、元のナビゲーション画像を最初に表示されたものよりも大きく保持します。
        navImageWidth = (int) (getWidth() * NAV_IMAGE_FACTOR);
        navImageHeight = navImageWidth * image.getHeight() / image.getWidth();
        int scrNavImageWidth = (int) (getWidth() * SCREEN_NAV_IMAGE_FACTOR);
        @SuppressWarnings("unused")
        int scrNavImageHeight = scrNavImageWidth * image.getHeight() / image.getWidth();
        navScale = (double) scrNavImageWidth / navImageWidth;
        navigationImage = new BufferedImage(navImageWidth, navImageHeight,
                image.getType());
        Graphics g = navigationImage.getGraphics();
        g.drawImage(image, 0, 0, navImageWidth, navImageHeight, null);
    }

    /**
     * パネルに表示する画像を設定
     *
     * @param image パネルに設定する画像
     */
    public void setImage(BufferedImage image) {
        BufferedImage oldImage = this.image;
        this.image = image;
        //新しい画像のpaintComponent()でinitializeParameters()が呼び出されるように、スケールをリセット
        scale = 0.0;
        firePropertyChange(IMAGE_CHANGED_PROPERTY, oldImage, image);
        repaint();
    }

    /**
     * <画像が標準のRGB色空間を使用しているかどうかをテスト
     */
    @SuppressWarnings("unused")
    public static boolean isStandardRGBImage(BufferedImage bImage) {
        return bImage.getColorModel().getColorSpace().isCS_sRGB();
    }

    /**
     * このパネルの座標を元の画像の座標に変換
     * @param p パネルの座標
     * @return 画像の座標
     */
    private Coords panelToImageCoords(Point p) {
        return new Coords((p.x - originX) / scale, (p.y - originY) / scale);
    }

    /**
     * 元の画像の座標をこのパネルの座標に変換
     * @param p 画像の座標
     * @return パネルの座標
     */
    private Coords imageToPanelCoords(Coords p) {
        return new Coords((p.x * scale) + originX, (p.y * scale) + originY);
    }

    /**
     * ナビゲーション画像の座標をズームされた画像の座標に変換
     * @param p ナビゲーション画像の座標
     * @return ズームされた画像の座標
     */
    private Point navToZoomedImageCoords(Point p) {
        int x = p.x * getScreenImageWidth() / getScreenNavImageWidth();
        int y = p.y * getScreenImageHeight() / getScreenNavImageHeight();
        return new Point(x, y);
    }

    /**
     * ユーザーがナビゲーション画像内をクリックすると、画像のこの部分がパネルに表示されます。
     * 画像のクリックされたポイントは、パネルの中央に配置
     * @param p 画像のクリックされたポイント
     */
    private void displayImageAt(Point p) {
        Point scrImagePoint = navToZoomedImageCoords(p);
        originX = -(scrImagePoint.x - getWidth() / 2);
        originY = -(scrImagePoint.y - getHeight() / 2);
        repaint();
    }

    /**
     * パネル内の特定のポイントが画像の境界内にあるかどうかをテスト
     * @param p パネル内の特定のポイント
     * @return 結果
     */
    private boolean isInImage(Point p) {
        Coords coords = panelToImageCoords(p);
        int x = coords.getIntX();
        int y = coords.getIntY();
        return (x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight());
    }

    /**
     * パネル内の特定のポイントがナビゲーション画像の境界内にあるかどうかをテスト
     * @param p パネル内の特定のポイント
     * @return 結果
     */
    private boolean isInNavigationImage(Point p) {
        return (isNavigationImageEnabled() && p.x < getScreenNavImageWidth()
                && p.y < getScreenNavImageHeight());
    }

    /**
     * 画像のサイズが変更されるときに使用
     * @return 結果
     */
    private boolean isImageEdgeInPanel() {
        if (previousPanelSize == null) {
            return false;
        }

        return (originX > 0 && originX < previousPanelSize.width
                || originY > 0 && originY < previousPanelSize.height);
    }

    /**
     * 画像全体がパネルに表示されているかどうかをテスト
     * @return 結果
     */
    private boolean isFullImageInPanel() {
        return (originX >= 0 && (originX + getScreenImageWidth()) < getWidth()
                && originY >= 0 && (originY + getScreenImageHeight()) < getHeight());
    }

    /**
     * 高品質のレンダリング機能が有効になっているかどうかテスト
     *
     * @return 高品質のレンダリングが有効になっている場合はtrue、それ以外の場合はfalse
     */
    @SuppressWarnings("unused")
    public boolean isHighQualityRenderingEnabled() {
        return highQualityRenderingEnabled;
    }

    /**
     * 高品質のレンダリングを有効/無効にします。
     *
     * @param enabled 高品質のレンダリングを有効:true / 無効:false
     */
    public void setHighQualityRenderingEnabled(boolean enabled) {
        highQualityRenderingEnabled = enabled;
    }

    /**
     * 拡大縮小された画像が元の画像よりも大きい場合、高品質のレンダリングが開始されます。
     * つまり、画像のデシメーションが停止し、補間が開始されたときです。
     * @return 結果
     */
    private boolean isHighQualityRendering() {
        return (highQualityRenderingEnabled
                && scale > HIGH_QUALITY_RENDERING_SCALE_THRESHOLD);
    }

    /**
     * ナビゲーション画像が有効かどうかを示
     *
     * @return ナビゲーション画像が有効な場合はtrue、それ以外の場合はfalse
     */
    private boolean isNavigationImageEnabled() {
        return navigationImageEnabled;
    }

    /**
     * ナビゲーション画像によるナビゲーションを有効/無効にします
     * カスタムのプログラムナビゲーションを実装する場合は、ナビゲーション画像を無効にする必要があります。
     *
     * @param enabled ナビゲーション画像が有効な場合はtrue、それ以外の場合はfalse
     */
    @SuppressWarnings("SameParameterValue")
    private void setNavigationImageEnabled(boolean enabled) {
        navigationImageEnabled = enabled;
        repaint();
    }

    /**
     * パネルのサイズが変更されたときに使用されます
     */
    private void scaleOrigin() {
        originX = originX * getWidth() / previousPanelSize.width;
        originY = originY * getHeight() / previousPanelSize.height;
        repaint();
    }

    /**
     * 指定されたズームレベルをスケールに変換
     * @param zoom ズームレベル
     * @return スケール
     */
    private double zoomToScale(double zoom) {
        return initialScale * zoom;
    }

    /**
     * 現在のズームレベルを取得
     *
     * @return 現在のズームレベル
     */
    private double getZoom() {
        return scale / initialScale;
    }

    /**
     * 画像の表示に使用するズームレベルを設定
     * この方法は、プログラムによるズームで使用されます。
     * ズームの中心は、パネルの中心に最も近い画像のポイントです。
     * 新しいズームレベルが設定された後、画像が再描画されます。
     *
     * @param newZoom パネルの画像を表示するために使用されるズームレベル
     */
    @SuppressWarnings("unused")
    public void setZoom(double newZoom) {
        Point zoomingCenter = new Point(getWidth() / 2, getHeight() / 2);
        setZoom(newZoom, zoomingCenter);
    }

    /**
     * 画像の表示に使用するズームレベルと、ズームが行われるズームセンターを設定
     * この方法は、プログラムによるズームで使用されます。
     * 新しいズームレベルが設定されると、画像が再描画されます。
     *
     * @param newZoom パネルの画像を表示するために使用されるズームレベル
     */
    private void setZoom(double newZoom, Point zoomingCenter) {
        Coords imageP = panelToImageCoords(zoomingCenter);
        if (imageP.x < 0.0) {
            imageP.x = 0.0;
        }
        if (imageP.y < 0.0) {
            imageP.y = 0.0;
        }
        if (imageP.x >= image.getWidth()) {
            imageP.x = image.getWidth() - 1.0;
        }
        if (imageP.y >= image.getHeight()) {
            imageP.y = image.getHeight() - 1.0;
        }

        Coords correctedP = imageToPanelCoords(imageP);
        double oldZoom = getZoom();
        scale = zoomToScale(newZoom);
        Coords panelP = imageToPanelCoords(imageP);

        originX += (correctedP.getIntX() - (int) panelP.x);
        originY += (correctedP.getIntY() - (int) panelP.y);

        firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, new Double(oldZoom),
                new Double(getZoom()));

        repaint();
    }

    /**
     * 現在のズーム増分を取得
     *
     * @return 現在のズーム増分
     */
    @SuppressWarnings("unused")
    public double getZoomIncrement() {
        return zoomIncrement;
    }

    /**
     * 新しいズーム増分値を設定
     *
     * @param newZoomIncrement 新しいズーム増分値
     */
    @SuppressWarnings("unused")
    public void setZoomIncrement(double newZoomIncrement) {
        double oldZoomIncrement = zoomIncrement;
        zoomIncrement = newZoomIncrement;
        firePropertyChange(ZOOM_INCREMENT_CHANGED_PROPERTY,
                new Double(oldZoomIncrement), new Double(zoomIncrement));
    }

    /**
     * パネル内の画像を新しいズームレベルで再描画してズームします。
     * 現在のマウスの位置はズームの中心です。
     */
    private void zoomImage() {
        Coords imageP = panelToImageCoords(mousePosition);
        double oldZoom = getZoom();
        scale *= zoomFactor;
        Coords panelP = imageToPanelCoords(imageP);

        originX += (mousePosition.x - (int) panelP.x);
        originY += (mousePosition.y - (int) panelP.y);

        firePropertyChange(ZOOM_LEVEL_CHANGED_PROPERTY, new Double(oldZoom),
                new Double(getZoom()));

        repaint();
    }

    /**
     * ナビゲーション画像をズーム
     */
    private void zoomNavigationImage() {
        navScale *= navZoomFactor;
        repaint();
    }

    /**
     * 画像の原点を取得
     * 画像の原点は、パネルの座標系での画像の左上隅として定義されます。
     *
     * @return パネルの座標系における画像の左上隅のポイント
     */
    @SuppressWarnings("unused")
    public Point getImageOrigin() {
        return new Point(originX, originY);
    }

    /**
     * 画像の原点を設定
     * 画像の原点は、パネルの座標系での画像の左上隅として定義されます。
     * 新しい原点が設定された後、画像が再描画されます。
     * このメソッドは、プログラムによる画像ナビゲーションに使用されます。
     *
     * @param x 新しい画像の原点のx座標
     * @param y 新しい画像の原点のy座標
     */
    @SuppressWarnings("unused")
    public void setImageOrigin(int x, int y) {
        setImageOrigin(new Point(x, y));
    }

    /**
     * 画像の原点を設定
     * 画像の原点は、パネルの座標系での画像の左上隅として定義されます。
     * 新しい原点が設定された後、画像が再描画されます。
     * このメソッドは、プログラムによる画像ナビゲーションに使用されます。
     *
     * @param newOrigin 新しい画像の原点の値
     */
    private void setImageOrigin(Point newOrigin) {
        originX = newOrigin.x;
        originY = newOrigin.y;
        repaint();
    }

    /**
     * （マウスでドラッグして）画像を新しいマウス位置pに移動します。
     * @param p 新しいマウス位置
     */
    private void moveImage(Point p) {
        int xDelta = p.x - mousePosition.x;
        int yDelta = p.y - mousePosition.y;
        originX += xDelta;
        originY += yDelta;
        mousePosition = p;
        repaint();
    }

    /**
     * パネルに現在表示されている画像領域の境界を取得します（画像座標で）。
     * @return 画像座標
     */
    private Rectangle getImageClipBounds() {
        Coords startCoords = panelToImageCoords(new Point(0, 0));
        Coords endCoords = panelToImageCoords(new Point(getWidth() - 1, getHeight() - 1));
        int panelX1 = startCoords.getIntX();
        int panelY1 = startCoords.getIntY();
        int panelX2 = endCoords.getIntX();
        int panelY2 = endCoords.getIntY();
        //交差点はありませんか？
        if (panelX1 >= image.getWidth() || panelX2 < 0 || panelY1 >= image.getHeight() || panelY2 < 0) {
            return null;
        }

        int x1 = Math.max(panelX1, 0);
        int y1 = Math.max(panelY1, 0);
        int x2 = (panelX2 >= image.getWidth()) ? image.getWidth() - 1 : panelX2;
        int y2 = (panelY2 >= image.getHeight()) ? image.getHeight() - 1 : panelY2;
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    private TexturePaint paint;

    /**
     * パネルとその画像を、画像の縮尺に応じて、現在のズームレベル、位置、および補間方法でペイントします。
     *
     * @param g ペイントのためのグラフィックスコンテキスト
     */
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // 背景をペイントします

        if (paint == null)
            paint = new TexturePaint(bg, new Rectangle(10, 10));

        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint(paint);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        if (image == null) {
            return;
        }

        if (scale == 0.0) {
            initializeParams();
        }

        if (isHighQualityRendering()) {
            Rectangle rect = getImageClipBounds();
            if (rect == null || rect.width == 0 || rect.height == 0) { // パネルに画像の一部が表示されない
                return;
            }

            BufferedImage subImage = image.getSubimage(rect.x, rect.y, rect.width,
                    rect.height);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, INTERPOLATION_TYPE);
            g2.drawImage(subImage, Math.max(0, originX), Math.max(0, originY),
                    Math.min((int) (subImage.getWidth() * scale), getWidth()),
                    Math.min((int) (subImage.getHeight() * scale), getHeight()), null);
        } else {
            g.drawImage(image, originX, originY, getScreenImageWidth(),
                    getScreenImageHeight(), null);
        }

        //ナビゲーション画像を描く
        if (isNavigationImageEnabled()) {
            g.drawImage(navigationImage, 0, 0, getScreenNavImageWidth(),
                    getScreenNavImageHeight(), null);
            drawZoomAreaOutline(g);
        }
    }

    /**
     * パネルに現在表示されている画像の領域を示すナビゲーション画像の上に白い輪郭を描きます。
     * @param g グラフィックスコンテキスト
     */
    private void drawZoomAreaOutline(Graphics g) {
        if (isFullImageInPanel()) {
            return;
        }

        int x = -originX * getScreenNavImageWidth() / getScreenImageWidth();
        int y = -originY * getScreenNavImageHeight() / getScreenImageHeight();
        int width = getWidth() * getScreenNavImageWidth() / getScreenImageWidth();
        int height = getHeight() * getScreenNavImageHeight() / getScreenImageHeight();
        g.setColor(Color.white);
        g.drawRect(x, y, width, height);
    }

    private int getScreenImageWidth() {
        return (int) (scale * image.getWidth());
    }

    private int getScreenImageHeight() {
        return (int) (scale * image.getHeight());
    }

    private int getScreenNavImageWidth() {
        return (int) (navScale * navImageWidth);
    }

    private int getScreenNavImageHeight() {
        return (int) (navScale * navImageHeight);
    }

    private static String[] getImageFormatExtensions() {
        String[] names = ImageIO.getReaderFormatNames();
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].toLowerCase();
        }
        Arrays.sort(names);
        return names;
    }

    @SuppressWarnings("unused")
    private static boolean endsWithImageFormatExtension(String name) {
        int dotIndex = name.lastIndexOf(".");
        if (dotIndex == -1) {
            return false;
        }

        String extension = name.substring(dotIndex + 1).toLowerCase();
        return (Arrays.binarySearch(getImageFormatExtensions(), extension) >= 0);
    }


}
