package com.fragmenterworks.ffxivextract.models.uldStuff.renderer;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.helpers.FileTools;
import com.fragmenterworks.ffxivextract.helpers.ImageDecoding;
import com.fragmenterworks.ffxivextract.helpers.SparseArray;
import com.fragmenterworks.ffxivextract.helpers.Utils;
import com.fragmenterworks.ffxivextract.models.*;
import com.fragmenterworks.ffxivextract.models.uldStuff.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.*;

/**
 * Created by Roze on 2017-06-23.
 *
 * @author Roze
 */
public class ULD_File_Renderer implements MouseListener, MouseMotionListener {

    //他のファイルを見つけるために使用されます
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private static SqPack_IndexFile currentIndex;

    private final static Map<Integer, Class<? extends GraphicsElement<? extends GraphicsNode>>> graphicsTypes = new HashMap<>();
    private final static Map<Integer, Class<? extends UIComponent<?>>> uiComponentTypes = new HashMap<>();

    static {
        graphicsTypes.put(1, GraphicsContainer.class);
        graphicsTypes.put(2, GraphicsImage.class);
        graphicsTypes.put(3, GraphicsTextBox.class);
        graphicsTypes.put(4, GraphicsMultiImage.class);

        uiComponentTypes.put(0, NullUIComponent.class);
        uiComponentTypes.put(1, NullUIComponent.class);
        uiComponentTypes.put(2, CoFrame.class);
        uiComponentTypes.put(3, NullUIComponent.class);
        uiComponentTypes.put(4, NullUIComponent.class);
        uiComponentTypes.put(5, NullUIComponent.class);
        uiComponentTypes.put(6, CoSlider.class);
        uiComponentTypes.put(7, NullUIComponent.class);
        uiComponentTypes.put(8, NullUIComponent.class);
        uiComponentTypes.put(9, CoList.class);
        uiComponentTypes.put(10, NullUIComponent.class);
        uiComponentTypes.put(11, NullUIComponent.class);
        uiComponentTypes.put(12, NullUIComponent.class);
        uiComponentTypes.put(13, CoScrollbar.class);
        uiComponentTypes.put(14, NullUIComponent.class);
        uiComponentTypes.put(15, NullUIComponent.class);
        uiComponentTypes.put(16, NullUIComponent.class);
        uiComponentTypes.put(17, NullUIComponent.class);
        uiComponentTypes.put(18, NullUIComponent.class);
        uiComponentTypes.put(19, NullUIComponent.class);
        uiComponentTypes.put(20, NullUIComponent.class);
        uiComponentTypes.put(21, NullUIComponent.class);
        uiComponentTypes.put(22, NullUIComponent.class);
        uiComponentTypes.put(23, NullUIComponent.class);
        uiComponentTypes.put(24, NullUIComponent.class);
        uiComponentTypes.put(25, NullUIComponent.class);
    }

    final private Map<Integer, BufferedImage> images = new HashMap<>();
    final private Map<Integer, TextureSet> textureSets = new HashMap<>();
    final private Map<Integer, GraphicsElement<? extends GraphicsNode>> graphics = new HashMap<>();
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    final private Map<Integer, IGraphicsElement> nodesByAccessor = new HashMap<>();

    private int width;
    private int height;
    private static final boolean PAINT_DEBUG = false;

    /**
     * 指定されたULDファイルからこのレンダラーを初期化します
     *
     * @param uld_file  以前に解析されたULDファイル
     */
    public ULD_File_Renderer(ULD_File uld_file) {

        if (uld_file.uldHeader.atkhs[1].wdhd != null){
            this.width = uld_file.uldHeader.atkhs[1].wdhd.getEntries().get(1).width;
            this.height = uld_file.uldHeader.atkhs[1].wdhd.getEntries().get(1).height;
        }else{
            this.width = 0;
            this.height = 0;
        }
        if(uld_file.spIndex == null){
            currentIndex = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\060000.win32.index", true);
        }else {
            currentIndex = uld_file.spIndex;
        }
        initTextures(uld_file.uldHeader.atkhs[0]);
        initTextureRegions(uld_file.uldHeader.atkhs[0]);
        initGraphics(uld_file);
    }

    private static GraphicsElement<? extends GraphicsNode> createElementByType(ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, int type, final SparseArray<COHDEntry> components) {
        if (type > 1000) {
            COHDEntry cohd = components.get(type);
            UIComponent<? extends COHDEntryType> uiComponent = createUIComponentType(renderer, cohd.index, cohd.type);
            GraphicsComponent element = new GraphicsComponent();
            element.setParent(parent);
            element.setComponent(uiComponent);
            return element;
        }
        if (graphicsTypes.containsKey(type)) {
            Class<? extends GraphicsElement<? extends GraphicsNode>> aClass = graphicsTypes.get(type);
            try {
                Constructor<? extends GraphicsElement<? extends GraphicsNode>> constructor = aClass.getDeclaredConstructor();
                return constructor.newInstance();
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
//				Utils.getGlobalLogger().error(e);
                return null;
            }
        }
        Constructor<? extends GraphicsElement<? extends GraphicsNode>> constructor;
        try {
            constructor = GraphicsContainer.class.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
//			Utils.getGlobalLogger().error(e);
        }
        return null;
    }

    /**
     * 構文解析をテストするためのメインルーチン
     *
     * @param args プログラム引数
     */
    public static void main(String[] args) {
        SqPack_IndexFile index;
        index = new SqPack_IndexFile(Constants.datPath + "\\game\\sqpack\\ffxiv\\060000.win32.index", true);
        byte[] data = index.extractFile("ui/uld/creditstaff.uld");

        ULD_File uld = new ULD_File(index, data, ByteOrder.LITTLE_ENDIAN);

        Utils.getGlobalLogger().trace(uld);
        ULD_File_Renderer renderer = new ULD_File_Renderer(uld);
        JFrame jf = new JFrame();
        JPanel content = new JPanel();
        jf.setContentPane(content);
        content.setLayout(new BorderLayout());
        JLabel lblPic = new JLabel();
        lblPic.setIcon(new ImageIcon(renderer.getImage(0, 0)));
        lblPic.addMouseListener(renderer);
        lblPic.addMouseMotionListener(renderer);
        lblPic.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                renderer.getImage(0, 0);
                lblPic.repaint();
            }
        });

        content.add(lblPic, BorderLayout.CENTER);
        jf.pack();
        //FrameUtilities.centerFrame(jf);
        jf.setVisible(true);
    }

    @SuppressWarnings("unused")
    private static UIComponent<? extends COHDEntryType> createUIComponentType(ULD_File_Renderer renderer, int index, int type) {
        if (uiComponentTypes.containsKey(type)) {
            Class<? extends UIComponent<? extends COHDEntryType>> aClass = uiComponentTypes.get(type);
            try {
                Constructor<? extends UIComponent<? extends COHDEntryType>> constructor = aClass.getDeclaredConstructor(int.class);
                return constructor.newInstance(index);
            } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException ignored) {

            }
        }else{
            Utils.getGlobalLogger().trace(String.format("未知のType「%d」があります", type));
        }
        return null;
    }

    private static GraphicsElement<? extends GraphicsNode> createElementByIndex(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, int index, SparseArray<? extends GraphicsNode> nodes, Map<Integer, GraphicsElement<?>> elements, SparseArray<COHDEntry> components) {
        if (index == 0) {
            return null;
        }
        if (!elements.containsKey(index)) {
            GraphicsNode node = nodes.get(index);
            if (node != null) {
                GraphicsElement<? extends GraphicsNode> element = createElementByType(renderer, parent, node.type, components);
                if (element != null) {
                    elements.put(node.index, element);
                    element.load(renderer, node, nodes, elements, components);
                    if (element instanceof GraphicsComponent) {
                        if (((GraphicsComponent) element).component != null) {
                            if (components != null) {
                                COHDEntry cohd = components.get(node.type);
                                //noinspection unchecked
                                ((GraphicsComponent) element).component.load(renderer, element, cohd, components, cohd.typeData);
                            }else{
                                Utils.getGlobalLogger().trace("SparseArray<COHDEntry> componentsがnull");
                            }
                        }
                    }
                    return element;
                }
            }
        } else {
            return elements.get(index);
        }
        return null;
    }

    private void initGraphics(final ULD_File uld_file) {
        SparseArray<WDHDEntry> entries = uld_file.uldHeader.atkhs[1].wdhd.getEntries();

        @SuppressWarnings("unused")
        final SparseArray<WDHDEntry> wdhd = uld_file.uldHeader.atkhs[1].wdhd.getEntries();
        for (int i = 0; i < entries.size(); i++) {
            int key = entries.keyAt(i);
            WDHDEntry cohd = entries.get(key);
            if (uld_file.uldHeader.atkhs[0].cohd == null){
                continue;
            }
            GraphicsElement<? extends GraphicsNode> node = createElementByIndex(this, null, 1, cohd.nodes, new HashMap<>(), uld_file.uldHeader.atkhs[0].cohd.getEntries());
            if (node != null) {
                graphics.put(cohd.index, node);
            }
        }
    }

    /**
     * TPHDチャンクのすべてのテクスチャセットを初期化し、initTexturesによって以前にロードされた画像にこれらをバインドします
     *
     * @param atkh 対象のTPHDチャンクを保持するATKHチャンク
     */
    private void initTextureRegions(final ULD_File.ATKH atkh) {
        if (atkh.tphd == null){
            return;
        }

        SparseArray<ImageSet> imageSets = atkh.tphd.imageSets;
        for (int i = 0; i < imageSets.size(); i++) {
            @SuppressWarnings("unused")
            int key = imageSets.keyAt(i);
            ImageSet set = imageSets.valueAt(i);
            int index = set.index;
            TextureSet tset = new TextureSet(index);
            for (ImageSetRegion region : set.regions) {
                tset.addRegion(new TextureRegion(images.get(region.imageIndex), region.x, region.y, region.w, region.h));
            }
            textureSets.put(index, tset);
        }
    }

    /**
     * ASHDチャンクに記述されているすべてのテクスチャを初期化してロードします
     *
     * @param atkh      ASHDチャンクを持っているATKHチャンク
     */
    private void initTextures(final ULD_File.ATKH atkh) {
        if (atkh.ashd == null){
            return;
        }

        SparseArray<String> paths = atkh.ashd.paths;
        if (atkh.ashd.ashd_Ver.equals("1.00")) {
            for (int i = 0; i < paths.size(); i++) {
                int key = paths.keyAt(i);
                String path = paths.valueAt(i);
                if (path.length() == 2) {
                    ByteBuffer bb = ByteBuffer.wrap(path.getBytes());
                    //noinspection ResultOfMethodCallIgnored
                    bb.order();
                    BufferedImage img = FileTools.getIcon((int) bb.getShort() & 0xFFFF);
                    if (img != null) {
                        images.put(key, img);
                    }
                }
                BufferedImage texture = FileTools.getTexture(path);
                //ImageDebug.addImage(texture)
                images.put(key, texture);
            }
        }else{
            //ashd_Verが1.01以上
            for (int i = 0; i < paths.size(); i++) {
                int key = paths.keyAt(i);
                String path = paths.valueAt(i);
                try {
                    if(currentIndex.findFile(path) == 2) {
                        byte[] data = currentIndex.extractFile(path);
                        Texture_File tex = new Texture_File(data, currentIndex.getEndian());
                        BufferedImage texture = tex.decode(0, null);
                        images.put(key, texture);
                    }
                } catch (ImageDecoding.ImageDecodingException e) {
                    Utils.getGlobalLogger().info("テクスチャ読み込み時にエラーが発生");
                    e.printStackTrace();
                }
            }
        }


    }

    private BufferedImage bi = null;
    private Graphics2D biGraphics;

    public BufferedImage getImage(int width, int height) {
        if (width != 0) {
            this.width = width;
        }
        if (height != 0) {
            this.height = height;
        }
        if (graphics.size()==0){
            if (bi == null) {
                if (this.width <= 0){
                    this.width = 32;
                }
                if (this.height <= 0){
                    this.height = 32;
                }
                bi = new BufferedImage(this.width, this.height, BufferedImage.TYPE_4BYTE_ABGR);
                biGraphics = bi.createGraphics();
                biGraphics.setColor(Color.LIGHT_GRAY);
                return bi;
            }
        }

        List<GraphicsElement<? extends GraphicsNode>> values = new ArrayList<>(graphics.values());
        Collections.reverse(values); //List「values」を逆順にする
        Rectangle bounds = new Rectangle(); //矩形オブジェクト作成
        for (GraphicsElement<? extends GraphicsNode> g : values) {
            g.getMaxBounds(bounds, 0, 0, g.width, g.height);
        }

        int imgWidth = Math.abs(bounds.x) + Math.abs(bounds.width);
        int imgHeight = Math.abs(bounds.y) + Math.abs(bounds.height);
        @SuppressWarnings("unused")
        int containerWidth = bounds.width - bounds.x;
        @SuppressWarnings("unused")
        int containerHeight = bounds.height - bounds.y;
        int containerTop = Math.abs(bounds.y);
        int containerLeft = Math.abs(bounds.x);
        if (bi == null) {
            bi = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_4BYTE_ABGR);
            biGraphics = bi.createGraphics();
            AffineTransform tx = new AffineTransform();
            tx.translate(containerLeft, containerTop);
            biGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //アンチエイリアス
            biGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); //テキスト・アンチエイリアス
            biGraphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); //補間
            biGraphics.setTransform(tx);
        }
        biGraphics.setColor(Color.LIGHT_GRAY);
        //biGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
        biGraphics.fillRect(-containerLeft, -containerTop, bi.getWidth(), bi.getHeight());
        biGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
        for (GraphicsElement<? extends GraphicsNode> g : values) {
            g.paint(biGraphics, imgWidth, imgHeight);
        }
        return bi;
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
        Utils.getGlobalLogger().trace("uld::mouseClicked(" + e.getPoint() + ")");
        LinkedList<GraphicsElement<? extends GraphicsNode>> list = new LinkedList<>(graphics.values());
        while (list.size() > 0) {
            GraphicsElement<? extends GraphicsNode> container = list.pollFirst();
            GraphicsElement<? extends GraphicsNode> node = container.lastChild;
            while (node != null) {
                if (node.drawn.contains(e.getPoint())) { //node.drawX <= e.getX() && node.drawX + node.drawWidth >= e.getX() && node.drawY <= e.getY() && node.drawY + node.drawHeight >= e.getY()
                    if (node instanceof MouseListener) {
                        ((MouseListener) node).mouseClicked(e);
                        if (e.isConsumed()) {
                            list.clear();
                            break;
                        }
                    } else {
                        Utils.getGlobalLogger().trace(node._getPathString());
                    }
                }
                if (node.lastChild != null) {
                    list.add(node);
                }
                node = node.previousItem;
            }
        }
        Utils.getGlobalLogger().trace("uld::mouseClicked_end");

    }

    @Override
    public void mousePressed(final MouseEvent e) {

    }

    @Override
    public void mouseReleased(final MouseEvent e) {

    }

    @Override
    public void mouseEntered(final MouseEvent e) {

    }

    @Override
    public void mouseExited(final MouseEvent e) {

    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        Utils.getGlobalLogger().trace("uld::mouseDragged(" + e.getPoint() + ")");
        LinkedList<GraphicsElement<? extends GraphicsNode>> list = new LinkedList<>(graphics.values());
        while (list.size() > 0) {
            GraphicsElement<? extends GraphicsNode> container = list.pollFirst();
            GraphicsElement<? extends GraphicsNode> node = container.lastChild;
            while (node != null) {
                if (node.drawn.contains(e.getPoint())) { //node.drawX <= e.getX() && node.drawX + node.drawWidth >= e.getX() && node.drawY <= e.getY() && node.drawY + node.drawHeight >= e.getY()
                    if (node instanceof MouseMotionListener) {
                        ((MouseMotionListener) node).mouseDragged(e);
                        if (e.isConsumed()) {
                            list.clear();
                            break;
                        }
                    }
                }
                if (node.lastChild != null) {
                    list.add(node);
                }
                node = node.previousItem;
            }
        }
        Utils.getGlobalLogger().trace("uld::mouseDragged_end");
    }

    @Override
    public void mouseMoved(final MouseEvent e) {

    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static abstract class UIComponent<T extends COHDEntryType> implements IGraphicsElement, MouseListener, MouseMotionListener {
        int index;
        GraphicsElement<? extends GraphicsNode> graphics;
        GraphicsElement<? extends GraphicsNode> parent;
        final Set<MouseListener> mouseListeners = new HashSet<>();
        final Set<MouseMotionListener> mouseMotionListeners = new HashSet<>();
        final Set<KeyListener> keyListeners = new HashSet<>();

        void addMouseListener(MouseListener listener) {
            mouseListeners.add(listener);
        }

        void addMouseMotionListener(MouseMotionListener listener) {
            mouseMotionListeners.add(listener);
        }

        @SuppressWarnings("unused")
        public void addKeyListener(KeyListener listener) {
            keyListeners.add(listener);
        }

        void fireMouseClickedEvent(MouseEvent e) {
            for (MouseListener l : mouseListeners) {
                try {
                    l.mouseClicked(e);
                } catch (Throwable t) {
//					t.printStackTrace();
                }
            }
        }

        void fireMouseDraggedEvent(MouseEvent e) {
            for (MouseMotionListener l : mouseMotionListeners) {
                try {
                    l.mouseDragged(e);
                } catch (Throwable t) {
//					t.printStackTrace();
                }
            }
        }

        final SparseArray<GraphicsElement<? extends GraphicsNode>> elementList = new SparseArray<>();
        int type;

        @SuppressWarnings("UnusedParameters")
        public UIComponent(int index) {

        }

        public int getIndex() {
            return index;
        }

        public void setIndex(final int index) {
            this.index = index;
        }

        /**
         * loadのスーパーメソッド
         * COHDEntry型によってオーバーロードされます
         * @param renderer レンダラー
         * @param parent 親コンポーネント
         * @param entry COHDEntry
         * @param components COHDEntry配列
         * @param typeData COHDEntryタイプ
         */
        //@Override
        void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, COHDEntry entry, final SparseArray<COHDEntry> components, final T typeData) {
            graphics = createElementByIndex(renderer, null, 1, entry.nodes, new HashMap<>(), components);
            type = entry.type;
            this.index = entry.index;
            this.parent = parent;
            if (graphics != null) {
                graphics.parent = parent;
                LinkedList<GraphicsElement<? extends GraphicsNode>> containers = new LinkedList<>();
                containers.add(graphics);
                while (containers.size() > 0) {
                    GraphicsElement<? extends GraphicsNode> container = containers.pollFirst();
                    GraphicsElement<? extends GraphicsNode> ge = container.lastChild;
                    while (ge != null) {
                        if (ge.lastChild != null) {
                            containers.add(ge);
                        }
                        elementList.put(ge.nodeIndex, ge);
                        ge = ge.previousItem;
                    }
                }
            }
        }

        public GraphicsElement<? extends GraphicsNode> getElement(int nodeIndex) {
            return (nodeIndex == 0 ? null : elementList.get(nodeIndex));
        }

        @Override
        public void paint(final Graphics2D g, final int width, final int height) {
            graphics.paint(g, width, height);
        }

        interface EventCallback {
            void callback(GraphicsElement<? extends GraphicsNode> node, MouseEvent event);
        }

        private void fireOnAllHits(MouseEvent event, EventCallback callback) {
            LinkedList<GraphicsElement<? extends GraphicsNode>> list = new LinkedList<>();
            list.add(graphics);
            while (list.size() > 0) {
                GraphicsElement<? extends GraphicsNode> container = list.pollFirst();
                GraphicsElement<? extends GraphicsNode> node = container.lastChild;
                while (node != null) {
                    Utils.getGlobalLogger().trace("\t" + node.nodeIndex + "[" + node.type + "]" + " :: " + node.drawn.getBounds());
                    if (node.drawn.contains(event.getPoint())) { //node.drawX <= e.getX() && node.drawX + node.drawWidth >= e.getX() && node.drawY <= e.getY() && node.drawY + node.drawHeight >= e.getY()
                        callback.callback(node, event);
                        if (event.isConsumed()) {
                            list.clear();
                            break;
                        }
                    }
                    if (node.lastChild != null) {
                        list.add(node);
                    }
                    node = node.previousItem;
                }
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            Utils.getGlobalLogger().trace("uiComponent[" + index + "]::mouseClicked(" + e.getPoint() + ")");
            //Utils.getGlobalLogger().trace("\t" + graphics.drawn.getBounds());
            fireMouseClickedEvent(e);
            if (!e.isConsumed()) {
                fireOnAllHits(e, (node, event) -> {
                    if (node instanceof MouseListener) {
                        ((MouseListener) node).mouseClicked(event);
                    } else {
                        Utils.getGlobalLogger().trace(node._getPathString());
                    }
                });
            }
            Utils.getGlobalLogger().trace("uiComponent[" + index + "]::mouseClicked_end");
        }

        @Override
        public void mouseMoved(final MouseEvent e) {

        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            fireMouseDraggedEvent(e);
            if (!e.isConsumed()) {
                fireOnAllHits(e, (node, event) -> {
                    if (node instanceof MouseMotionListener) {
                        ((MouseMotionListener) node).mouseDragged(event);
                    }
                });
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {

        }

        @Override
        public void mouseReleased(final MouseEvent e) {

        }

        @Override
        public void mouseEntered(final MouseEvent e) {

        }

        @Override
        public void mouseExited(final MouseEvent e) {

        }
    }

    public static abstract class GraphicsElement<T extends GraphicsNode> implements IGraphicsElement {

        protected GraphicsElement<? extends GraphicsNode> parent;
        protected GraphicsElement<? extends GraphicsNode> nextItem;
        protected GraphicsElement<? extends GraphicsNode> previousItem;
        protected GraphicsElement<? extends GraphicsNode> lastChild;
        protected GraphicsNodeTypeData sourceTypeData;

        @SuppressWarnings("unused")
        protected boolean enabled = true;
        protected boolean visible = true;

        @SuppressWarnings("unused")
        public void removeSelf() {
            if (previousItem != null && previousItem.nextItem == this) {
                previousItem.nextItem = nextItem;
            }
            if (nextItem != null && nextItem.previousItem == this) {
                nextItem.previousItem = previousItem;
            }
            previousItem = null;
            nextItem = null;
        }

        public void insertItemAfter(GraphicsElement<? extends GraphicsNode> element) {
            if (nextItem != null && nextItem.previousItem == this) {
                element.nextItem = nextItem;
                nextItem.previousItem = element;
            }
            element.previousItem = this;
            nextItem = element;
        }

        public void insertItemBefore(GraphicsElement<? extends GraphicsNode> element) {
            if (previousItem != null && previousItem.nextItem == this) {
                element.previousItem = element;
                previousItem.nextItem = element;
            }
            element.nextItem = this;
            previousItem = element;
        }

        @SuppressWarnings("unused")
        public void insertChildNodeFirst(GraphicsElement<? extends GraphicsNode> element) {
            GraphicsElement<? extends GraphicsNode> e = lastChild;
            GraphicsElement<? extends GraphicsNode> node = null;
            while (e != null) {
                node = e;
                e = e.previousItem;
            }
            if (node != null) {
                node.insertItemBefore(element);
            } else {
                this.lastChild = element;
            }
        }

        @SuppressWarnings("unused")
        public void insertChildNodeLast(GraphicsElement<? extends GraphicsNode> element) {
            if (lastChild != null) {
                lastChild.insertItemAfter(element);
            } else {
                this.lastChild = element;
            }
        }

        int nodeIndex;
        int left;
        int top;
        int width;
        int height;
        float rotation;
        float scaleX;
        float scaleY;
        int transformOriginX;
        int transformOriginY;
        int type;

        final private Rectangle2D drawbounds = new Rectangle();
        Shape drawn;

        public GraphicsElement() {

        }

        public void load(final ULD_File_Renderer renderer, GraphicsNode entry, SparseArray<? extends GraphicsNode> nodes, Map<Integer, GraphicsElement<?>> elements, SparseArray<COHDEntry> components) {
            previousItem = createElementByIndex(renderer, this, entry.previous, nodes, elements, components);
            parent = createElementByIndex(renderer, this, entry.parent, nodes, elements, components);
            nextItem = createElementByIndex(renderer, this, entry.next, nodes, elements, components);
            nodeIndex = entry.index;
            type = entry.type;
            sourceTypeData = entry.typeData;
            left = entry.x;
            top = entry.y;
            width = entry.w;
            height = entry.h;
            rotation = entry.rotation;
            scaleX = entry.scaleX;
            scaleY = entry.scaleY;
            transformOriginX = entry.xOrigin;
            transformOriginY = entry.yOrigin;
            lastChild = createElementByIndex(renderer, this, entry.last, nodes, elements, components);
            if (entry.layer != 0) {
                renderer.addNodeByAccessor(entry.layer, this);
            }
            if(width < 0 || height < 0){
                Utils.getGlobalLogger().trace("幅か高さが負の値を取っています");
            }

        }

        public void save(T entry) {

        }

        protected void applyGraphicsTransform(final Graphics2D g) {
            AffineTransform at = new AffineTransform(g.getTransform());
            //at.scale(this.scaleX * (filpX?-1:0), this.scaleY * (filpY?-1:0));
            at.translate(left, top);
            at.translate(transformOriginX, transformOriginY);
            at.scale(this.scaleX, this.scaleY);
            at.translate(-transformOriginX, -transformOriginY);
            at.rotate(Math.toRadians(this.rotation), this.transformOriginX, this.transformOriginY);
            drawbounds.setRect(0, 0, this.width, this.height); //Rectangle2D box = new Rectangle(this.width, this.height);
            drawn = at.createTransformedShape(drawbounds);
            g.setTransform(at);
        }

        protected String _getPathString() {
            return (parent != null ? parent._getPathString() + "." : "") + this.nodeIndex + "[" + type + "]";
        }

        @SuppressWarnings("unused")
        protected String _strpadleft(int text) {
            return strpadleft(Integer.toString(text), ' ', 8);
        }

        @SuppressWarnings("unused")
        protected String _strpadleft(float text) {
            return strpadleft(String.format("%.2f", text), ' ', 8);
        }

        @SuppressWarnings("unused")
        protected String _strpadleft(String text) {
            return strpadleft(text, ' ', 8);
        }

        @SuppressWarnings("SameParameterValue")
        protected String strpadleft(String text, char pad, int padLength) {
            char[] str = new char[padLength];
            char[] src = text.toCharArray();
            Arrays.fill(str, pad);
            int insert = Math.min(src.length, padLength);
            int q = padLength - src.length;
            System.arraycopy(src, 0, str, q, insert);
            return new String(str);
        }

        /**
         *
         * @param bounds (戻り値)矩形オブジェクトの最大値
         * @param offsetX X(横)方向オフセット
         * @param offsetY Y(縦)方向オフセット
         * @param width 幅
         * @param height 高さ
         */
        @SuppressWarnings("unused")
        public void getMaxBounds(Rectangle bounds, int offsetX, int offsetY, int width, int height) {
            if (bounds.x > offsetX + left) {
                bounds.x = offsetX + left;
            }
            if (bounds.y > offsetY + top) {
                bounds.y = offsetY + top;
            }
            if (bounds.width < offsetX + left + this.width) {
                bounds.width = offsetX + left + this.width;
            }
            if (bounds.height < offsetY + top + this.height) {
                bounds.height = offsetY + top + this.height;
            }
            GraphicsElement<? extends GraphicsNode> child = lastChild;
            while (child != null) {
                child.getMaxBounds(bounds, offsetX + left, offsetY + top, this.width, this.height);
                if (child.lastChild != null) {
                    child.lastChild.getMaxBounds(bounds, offsetX + left, offsetY + top, this.width, this.height);
                }
                child = child.previousItem;
            }
        }

        /**
         * paint(基本)
         * @param g Graphics2D
         * @param width 幅
         * @param height 高さ
         */
        @Override
        public void paint(final Graphics2D g, final int width, final int height) {
            if (!visible) {
                return;
            }
            //Utils.getGlobalLogger().trace("GraphicsElement:" + nodeIndex + ":paint(width=" + _strpadleft(width) + ", height=" + _strpadleft(height) + ")[x=" + _strpadleft(left) + ", y=" + _strpadleft(top) + ", w=" + _strpadleft(this.width) + ", h=" + _strpadleft(this.height) + ", scaleX=" + _strpadleft(scaleX) + ", scaleY=" + _strpadleft(scaleY) + ", rotation=" + _strpadleft(rotation) + ", origoX=" + _strpadleft(transformOriginX) + ", origoY=" + _strpadleft(transformOriginY) + "]");
            applyGraphicsTransform(g);
            if (PAINT_DEBUG) {
                g.setColor(Color.red);
                g.drawRect(0, 0, this.width, this.height);
                g.drawString(_getPathString(), 3, 14);
            }
            GraphicsElement<? extends GraphicsNode> child = lastChild;
            while (child != null) {
                //GraphicsElement child = (GraphicsElement)_child;
                Graphics2D g1 = (Graphics2D) g.create();
                try {
                    child.paint(g1, this.width, this.height);
                } catch (Exception e) {
                    //このあたりでエラーが多発
                    Utils.getGlobalLogger().error(e);
                } finally {
                    g1.dispose();
                }
                child = child.previousItem;
            }
        }
    }

    private <T extends GraphicsNode> void addNodeByAccessor(final int layer, final GraphicsElement<T> tGraphicsElement) {
        nodesByAccessor.put(layer, tGraphicsElement);
    }

    public static class GraphicsContainer extends GraphicsElement<GraphicsNode> {
        public GraphicsContainer() {
        }
    }

    public static class GraphicsTextBox extends GraphicsElement<GraphicsNode> {

        Font font = null;

        @SuppressWarnings("unused")
        private final String text = null;
        private Color foreground = null;

        public GraphicsTextBox() {
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsNode entry, final SparseArray<? extends GraphicsNode> nodes, final Map<Integer, GraphicsElement<?>> elements, final SparseArray<COHDEntry> components) {
            super.load(renderer, entry, nodes, elements, components);
            GraphicsNodeTypeData_3 td = (GraphicsNodeTypeData_3) entry.typeData;
            int fontSize = Objects.requireNonNull(td).fontSize;
            int fontIndex = td.fontNumber;
            switch (fontIndex) {
                case 5:
                case 4:
                case 3: {
                    font = new Font(Font.SERIF, Font.PLAIN, fontSize);
                    //font = FontTable.getFont(FontTable.JUPITER_FONT);
                    break;
                }
                case 0:
                case 1: {
                    font = new Font(Font.MONOSPACED, Font.PLAIN, fontSize);
                    //font = FontTable.getFont(FontTable.AXIS_FONT);
                    break;
                }
            }
            foreground = new Color(td.foreground_r, td.foreground_g, td.foreground_b, (td.foreground_a / 2));
        }

        @Override
        public void save(final GraphicsNode entry) {
            super.save(entry);
        }

        /**
         * TextBox描画
         * @param g Graphics2D
         * @param width 幅
         * @param height 高さ
         */
        @Override
        public void paint(final Graphics2D g, final int width, final int height) {
            super.paint(g, width, height);
            g.setColor(Color.yellow);
            g.drawRect(0, 0, this.width, this.height);
            if (font != null) {
                Font old = g.getFont();
                g.setFont(font);
                g.setColor(foreground);
                g.drawString(Integer.toString(nodeIndex), 0, g.getFontMetrics().getHeight());
                g.setFont(old);
            }
        }
    }

    public static class GraphicsMultiImage extends GraphicsElement<GraphicsNode> {

        TextureSet image;
        @SuppressWarnings("unused")
        private BufferedImage localImage;
        boolean stretchCenter;
        int borderIndex;
        int tphdIndex;
        int tphdRegion;
        private int paddingBottom;
        private int paddingRight;
        private int paddingLeft;
        private int paddingTop;

        public GraphicsMultiImage() {
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsNode entry, final SparseArray<? extends GraphicsNode> nodes, final Map<Integer, GraphicsElement<?>> elements, final SparseArray<COHDEntry> components) {
            super.load(renderer, entry, nodes, elements, components);
            GraphicsNodeTypeData_4 td = (GraphicsNodeTypeData_4) entry.typeData;
            image = renderer.textureSets.get(Objects.requireNonNull(td).tphdIndex);
            stretchCenter = td.stretchCenter > 0;
            borderIndex = td.borderIndex;
            paddingTop = td.u3;
            paddingLeft = td.u2;
            paddingRight = td.u5;
            paddingBottom = td.u4;
            tphdIndex = td.tphdIndex;
            tphdRegion = td.tphdRegion;
        }

        void tileImage(Graphics2D dst, BufferedImage src, int dx, int dy, int sx, int sy, int dw, int dh, int sw, int sh) {

            for (int _y = 0; _y < dh; _y += sh) {
                for (int _x = 0; _x < dw; _x += sw) {
                    int _sw = Math.min(dw - _x, sw);
                    int _sh = Math.min(dh - _y, sh);
                    dst.drawImage(src, dx + _x, dy + _y, dx + _x + _sw, dy + _y + _sh, sx, sy, sx + _sw, sy + _sh, null);
                }
            }
        }

        @Override
        public void paint(final Graphics2D g, final int _width, final int _height) {
            super.paint(g, width, height);
            int _row_width = (stretchCenter ? _width : width);
            int _row_height = (stretchCenter ? _height : height);

            if (borderIndex > 0) {
                TextureRegion ul = image.getRegion(0);
                TextureRegion uc = image.getRegion(1);
                TextureRegion ur = image.getRegion(2);
                TextureRegion cl = image.getRegion(3);
                TextureRegion cc = image.getRegion(4);
                TextureRegion cr = image.getRegion(5);
                TextureRegion dl = image.getRegion(6);
                TextureRegion dc = image.getRegion(7);
                TextureRegion dr = image.getRegion(8);
                g.drawImage(ul.getImage(), 0, 0, ul.w, ul.h, ul.x, ul.y, ul.x + ul.w, ul.y + ul.h, null);
                tileImage(g, uc.getImage(), ul.w, 0, uc.x, uc.y, _row_width - ul.w - ur.w, uc.h, uc.w, uc.h);
                g.drawImage(ur.getImage(), _row_width - ur.w, 0, _row_width - ur.w + ur.w, ur.h, ur.x, ur.y, ur.x + ur.w, ur.y + ur.h, null);

                tileImage(g, cl.getImage(), 0, ur.h, cl.x, cl.y, cl.w, _row_height - ul.h - dl.h, cl.w, cl.h);
                tileImage(g, cc.getImage(), cr.w, ur.h, cl.x, cl.y, _row_width - cl.w - cr.w, _row_height - ur.h - dr.h, cc.w, cc.h);
                tileImage(g, cr.getImage(), _row_width - cr.w, ur.h, cr.x, cr.y, cr.w, _row_height - ur.h - dr.h, cr.w, cl.h);

                g.drawImage(dl.getImage(), 0, _row_height - dl.h, dl.w, _row_height, dl.x, dl.y, dl.x + dl.w, dl.y + dl.h, null);
                tileImage(g, dc.getImage(), dl.w, _row_height - dl.h, dc.x, dc.y, _row_width - dl.w - dr.w, dc.h, dc.w, dc.h);
                g.drawImage(dr.getImage(), _row_width - ur.w, _row_height - dr.h, _row_width, _row_height, dr.x, dr.y, dr.x + dr.w, dr.y + dr.h, null);
            } else {
                TextureRegion region = image.getRegion(tphdRegion);
                if (paddingTop > region.h) {
                    paddingTop = paddingTop / (paddingTop / region.h);
                }
                if (paddingLeft > region.w) {
                    paddingLeft = paddingLeft / (paddingLeft / region.w);
                }
                if (paddingRight > region.w) {
                    paddingRight = paddingRight / (paddingRight / region.w);
                }
                if (paddingBottom > region.h) {
                    paddingBottom = paddingBottom / (paddingBottom / region.h);
                }
                int x1 = region.x;
                int x2 = region.x + paddingLeft;
                int x3 = region.x + region.w - paddingRight;
                int x4 = region.x + region.w;
                int y1 = region.y;
                int y2 = region.y + paddingTop;
                int y3 = region.y + region.h - paddingBottom;
                int y4 = region.y + region.h;
                int dx1 = 0;
                int dy1 = 0;
                int dx2 = paddingLeft;
                int dy2 = paddingTop;
                int dx3 = _row_width - paddingRight;
                int dy3 = _row_height - paddingBottom;
                Image image = region.getImage();
                //ImageDebug.addImage(new ImageIcon(image));
                if (dy2 != dy1) {
                    if (dx1 != dx2) {
                        g.drawImage(image, dx1, dy1, dx2, dy2, x1, y1, x2, y2, null);
                    }
                    if (dx2 != dx3) {
                        g.drawImage(image, dx2, dy1, dx3, dy2, x2, y1, x3, y2, null);
                    }
                    if (dx3 != _row_width) {
                        g.drawImage(image, dx3, dy1, _row_width, dy2, x3, y1, x4, y2, null);
                    }
                }
                if (dy3 != dy2) {
                    if (dx1 != dx2) {
                        g.drawImage(image, dx1, dy2, dx2, dy3, x1, y2, x2, y3, null);
                    }
                    if (dx2 != dx3) {
                        g.drawImage(image, dx2, dy2, dx3, dy3, x2, y2, x3, y3, null);
                    }
                    if (dx3 != _row_width) {
                        g.drawImage(image, dx3, dy2, _row_width, dy3, x3, y2, x4, y3, null);
                    }
                }
                if (_row_height != dy3) {
                    if (dx1 != dx2) {
                        g.drawImage(image, dx1, dy3, dx2, _row_height, x1, y3, x2, y4, null);
                    }
                    if (dx2 != dx3) {
                        g.drawImage(image, dx2, dy3, dx3, _row_height, x2, y3, x3, y4, null);
                    }
                    if (dx3 != _row_width) {
                        g.drawImage(image, dx3, dy3, _row_width, _row_height, x3, y3, x4, y4, null);
                    }
                }
            }

        }
    }

    public static class GraphicsImage extends GraphicsElement<GraphicsNode> {
        int fillMode = 0;
        boolean flipX = false;
        boolean flipY = false;
        TextureRegion image;
        String id;
        @SuppressWarnings("unused")
        protected BufferedImage localImage;

        public GraphicsImage() {
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsNode entry, final SparseArray<? extends GraphicsNode> nodes, final Map<Integer, GraphicsElement<?>> elements, final SparseArray<COHDEntry> components) {
            super.load(renderer, entry, nodes, elements, components);
            GraphicsNodeTypeData_2 td = (GraphicsNodeTypeData_2) entry.typeData;
            fillMode = Objects.requireNonNull(td).fillMode;
            flipX = td.flipX;
            flipY = td.flipY;

            TextureSet textureSet = renderer.textureSets.get(td.tphdIndex);
            if (textureSet != null) {
                image = textureSet.getRegion(td.tphdRegion);
            }
            id = td.tphdIndex + "." + td.tphdRegion;
        }

        @Override
        public void save(final GraphicsNode entry) {
            super.save(entry);
        }

        @Override
        public void paint(final Graphics2D g, int width, int height) {
            super.paint(g, width, height);
            if (image == null) {
                return;
            }
            width = this.width;
            height = this.height;
            int refX1 = image.getX();
            int refY1 = image.getY();
            int refW = image.getW();
            int refH = image.getH();
            int refX2 = refX1 + refW;
            int refY2 = refY1 + refH;
            int dstX1 = 0;
            int dstY1 = 0;
            int dstX2 = width;
            int dstY2 = height;
            if (flipX) {
                int t = refX1;
                refX1 = refX2;
                refX2 = t;
            }
            if (flipY) {
                int t = refY1;
                refY1 = refY2;
                refY2 = t;
            }

            switch (fillMode) {
                case 0:
                case 1: {
                    g.drawImage(image.getImage(), dstX1, dstY1, refW, refH, refX1, refY1, refX2, refY2, null);
                    break;
                }
                case 2: {
                    g.drawImage(image.getImage(), dstX1, dstY1, dstX2, dstY2, refX1, refY1, refX2, refY2, null);
                    break;
                }
                case 3: {
                    if (refW == width / 2) {
                        width /= 2;
                        dstX2 = width;
                        g.drawImage(image.getImage(), dstX1, dstY1, dstX2, dstY2, refX1, refY1, refX2, refY2, null);
                        g.drawImage(image.getImage(), dstX2, dstY1, dstX2 * 2, dstY2, refX2, refY1, refX1, refY2, null);
                    } else if (refH == height / 2) {
                        height /= 2;
                        dstY2 = height;
                        g.drawImage(image.getImage(), dstX1, dstY1, dstX2, dstY2, refX1, refY1, refX2, refY2, null);
                        g.drawImage(image.getImage(), dstX1, dstY2, dstX2, dstY2 * 2, refX1, refY2, refX2, refY1, null);
                    }
                }
            }
        }
    }

    public static class GraphicsComponent extends GraphicsElement<GraphicsNode> implements MouseListener {
        @SuppressWarnings("rawtypes")
        UIComponent component;
        GraphicsElement<? extends GraphicsNode> parent;

        void setParent(final GraphicsElement<? extends GraphicsNode> parent) {
            this.parent = parent;
            if (component != null) {
                component.parent = parent;
            }
        }

        void setComponent(final UIComponent<? extends COHDEntryType> component) {
            this.component = component;
            if (parent != null && component != null) {
                component.parent = parent;
            }
        }

        @Override
        protected String _getPathString() {
            return super._getPathString() + "(" + component.index + ")";
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsNode entry, final SparseArray<? extends GraphicsNode> nodes, final Map<Integer, GraphicsElement<?>> elements, final SparseArray<COHDEntry> components) {
            super.load(renderer, entry, nodes, elements, components);
        }

        @Override
        public void paint(final Graphics2D g, final int width, final int height) {
            applyGraphicsTransform(g);
            if (component != null) {
                //componentの型によってOverrideされたpaintメソッドが呼び出されるためnullは不可
                component.paint(g, width, height);
            }else{
                //この辺でエラーが発生していた
                Utils.getGlobalLogger().trace("componentがありません");
            }
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            Utils.getGlobalLogger().trace("graphicsComponent::mouseClicked(" + e.getPoint() + ")");
            if (component != null) {
                component.mouseClicked(e);
            }
            Utils.getGlobalLogger().trace("graphicsComponent::mouseClicked_end");
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if (component != null) {
                component.mousePressed(e);
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            if (component != null) {
                component.mouseReleased(e);
            }
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
            if (component != null) {
                component.mouseEntered(e);
            }
        }

        @Override
        public void mouseExited(final MouseEvent e) {
            if (component != null) {
                component.mouseExited(e);
            }
        }
    }

    public static class NullUIComponent extends UIComponent<COHDEntryType> {

        public NullUIComponent(final int index) {
            super(index);
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, final COHDEntry entry, final SparseArray<COHDEntry> components, final COHDEntryType typeData) {
            super.load(renderer, parent, entry, components, typeData);
        }

        @Override
        public void paint(final Graphics2D g, final int width, final int height) {
            super.paint(g, width, height);
        }
    }

    public static class CoFrame extends UIComponent<COHDEntryType_Frame> {

        GraphicsElement<? extends GraphicsNode> btnClose;
        GraphicsElement<? extends GraphicsNode> btnSettings;
        GraphicsElement<? extends GraphicsNode> btnMagnify;
        GraphicsElement<? extends GraphicsNode> btnHelp;
        GraphicsElement<? extends GraphicsNode> txtTitle;
        GraphicsElement<? extends GraphicsNode> txtSubtitle;
        GraphicsElement<? extends GraphicsNode> elemTitlebar;

        public CoFrame(final int index) {
            super(index);
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, final COHDEntry entry, final SparseArray<COHDEntry> components, final COHDEntryType_Frame typeData) {
            super.load(renderer, parent, entry, components, typeData);
            this.btnClose = getElement(typeData.refCloseButton);
            this.btnHelp = getElement(typeData.refHelpButton);
            this.btnMagnify = getElement(typeData.refMagnifyButton);
            this.btnSettings = getElement(typeData.refSettingButton);
            this.txtTitle = getElement(typeData.refTitleTextBox);
            this.txtSubtitle = getElement(typeData.refSubtitleTextBox);
            this.elemTitlebar = getElement(typeData.refTitleBar);
            GraphicsElement<? extends GraphicsNode> node9 = getElement(9);
            GraphicsElement<? extends GraphicsNode> node10 = getElement(10);

            int originalWidth = graphics.width;
            int originalHeight = graphics.height;
            int titlebarX = 0;
            if (elemTitlebar != null) {
                Rectangle r = new Rectangle();
                elemTitlebar.getMaxBounds(r, 0, 0, 0, 0);
                titlebarX = r.x;
            }
            if (btnClose != null) {
                btnClose.left = parent.width - (originalWidth - (titlebarX + btnClose.left));
            }
            if (btnSettings != null) {
                btnSettings.left = parent.width - (originalWidth - (titlebarX + btnSettings.left));
            }
            if (btnMagnify != null) {
                btnMagnify.left = parent.width - (originalWidth - (titlebarX + btnMagnify.left));
            }
            if (btnHelp != null) {
                btnHelp.left = parent.width - (originalWidth - (titlebarX + btnHelp.left));
            }
            if (txtTitle != null) {
                if(parent.width < (originalWidth - (titlebarX + txtTitle.width))){
                    Utils.getGlobalLogger().trace("幅が負の値を取りそうです");
                    //Todo:処理方法を検討
                }else{
                    txtTitle.width = parent.width - (originalWidth - (titlebarX + txtTitle.width));
                }
            }

            if(parent.width < (originalWidth - graphics.width)){
                Utils.getGlobalLogger().trace("幅が負の値を取りそうです");
                //Todo:処理方法を検討
            }else{
                graphics.width = parent.width - (originalWidth - graphics.width);
            }
            if(parent.height < (originalHeight - graphics.height)){
                Utils.getGlobalLogger().trace("高さが負の値を取りそうです");
                //Todo:処理方法を検討
            }else{
                graphics.height = parent.height - (originalHeight - graphics.height);
            }

            if (node9 != null) {

                if(parent.width < (originalWidth - node9.width)){
                    Utils.getGlobalLogger().trace("幅が負の値を取りそうです");
                    //Todo:処理方法を検討
                }else{
                    node9.width = parent.width - (originalWidth - node9.width);
                }
                if(parent.height < (originalHeight - node9.height)){
                    Utils.getGlobalLogger().trace("高さが負の値を取りそうです");
                    //Todo:処理方法を検討
                }else{
                    node9.height = parent.height - (originalHeight - node9.height);
                }
            }
            if (node10 != null) {
                if(parent.width < (originalWidth - node10.width)){
                    Utils.getGlobalLogger().trace("幅が負の値を取りそうです");
                    //Todo:処理方法を検討
                }else{
                    node10.width = parent.width - (originalWidth - node10.width);
                }

                if(parent.height < (originalHeight - node10.height)){
                    Utils.getGlobalLogger().trace("高さが負の値を取りそうです");
                    //Todo:処理方法を検討
                }else{
                    node10.height = parent.height - (originalHeight - node10.height);
                }
            }

            if (btnClose != null) {
                ((GraphicsComponent) btnClose).component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        Utils.getGlobalLogger().trace("Close Button of " + type + " pressed");
                        e.consume();
                    }
                });
            }
            if (btnHelp != null) {
                ((GraphicsComponent) btnHelp).component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        Utils.getGlobalLogger().trace("Help Button of " + type + " pressed");
                        e.consume();
                    }
                });
            }
            if (btnSettings != null) {
                ((GraphicsComponent) btnSettings).component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        Utils.getGlobalLogger().trace("Settings Button of " + type + " pressed");
                        e.consume();
                    }
                });
            }
            if (btnMagnify != null) {
                ((GraphicsComponent) btnMagnify).component.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(final MouseEvent e) {
                        Utils.getGlobalLogger().trace("Magnify Button of " + type + " pressed");
                        e.consume();
                    }
                });
            }
        }
    }

    public static class CoList extends UIComponent<COHDEntryType_List> {

        GraphicsElement<? extends GraphicsNode> elemScrollbar;
        GraphicsElement<? extends GraphicsNode> elemItemTemplate;

        @SuppressWarnings("unused")
        protected final List<Object> items = new ArrayList<>();

        public CoList(final int index) {
            super(index);
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, final COHDEntry entry, final SparseArray<COHDEntry> components, final COHDEntryType_List typeData) {
            super.load(renderer, parent, entry, components, typeData);
            GraphicsElement<?> all = parent;
            if (all instanceof GraphicsComponent) {
                all = ((GraphicsComponent) all).parent;
            }

            elemScrollbar = getElement(typeData.refScrollbar);
            elemItemTemplate = getElement(typeData.refItemTemplate);
            int originalWidth = graphics.width;
            int originalHeight = graphics.height;

            graphics.width = all.width;
            graphics.height = all.height;
            if(graphics.width < 0 || graphics.height < 0){
                Utils.getGlobalLogger().trace("幅か高さが負の値を取っています");
            }

            if (elemScrollbar != null) {
                elemScrollbar.left = all.width - (originalWidth - elemScrollbar.left);
                elemScrollbar.height = all.height - (originalHeight - elemScrollbar.height);
                CoScrollbar component = (CoScrollbar) ((GraphicsComponent) elemScrollbar).component;
                component.updateSize(graphics);
            }
        }
    }

    public static class CoScrollbar extends UIComponent<COHDEntryType_Scrollbar> {

        GraphicsElement<? extends GraphicsNode> elemValueTextbox;
        GraphicsElement<? extends GraphicsNode> elemKnob;
        GraphicsElement<? extends GraphicsNode> btnUp;
        GraphicsElement<? extends GraphicsNode> btnDown;

        int minimum = 0;
        int maximum = 100;
        int value = 0;

        @SuppressWarnings("unused")
        public void setSize(int width, int height) {
            int originalWidth = graphics.width;
            int originalHeight = graphics.height;
            Rectangle r = new Rectangle();
        }

        public CoScrollbar(final int index) {
            super(index);
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, final COHDEntry entry, final SparseArray<COHDEntry> components, final COHDEntryType_Scrollbar typeData) {
            super.load(renderer, parent, entry, components, typeData);
            GraphicsElement<? extends GraphicsNode> all = parent;
            if (all instanceof GraphicsComponent) {
                all = ((GraphicsComponent) all).parent;
            }
            btnDown = getElement(typeData.refDownButton);
            btnUp = getElement(typeData.refUpButton);
            elemKnob = getElement(typeData.refTrack);
            elemValueTextbox = getElement(typeData.refValueTextBox);
            if (all.height > 0) {
                updateSize(all);
            }
        }

        void updateSize(GraphicsElement<? extends GraphicsNode> all) {
            if (all instanceof GraphicsComponent) {
                all = ((GraphicsComponent) all).parent;
            }
            @SuppressWarnings("unused")
            int originalWidth = graphics.width;
            int originalHeight = graphics.height;

            graphics.height = all.height - (originalHeight - graphics.top);
            if (btnDown != null)
                btnDown.top = all.height - (originalHeight - btnDown.top);
            elemKnob.height = all.height - (originalHeight - elemKnob.top);
        }

        @SuppressWarnings("unused")
        public void setLimits(int min, int max) {
            if (min > max) {
                throw new RuntimeException("Invalid min(" + min + ")/max(" + max + ") values");
            }
            minimum = min;
            maximum = max;
            setValue(value);
        }

        public void setMax(int max) {
            if (minimum > max) {
                throw new RuntimeException("Invalid min(" + minimum + ")/max(" + max + ") values");
            }
            setValue(value);
        }

        public void setMin(int min) {
            if (min > maximum) {
                throw new RuntimeException("Invalid min(" + min + ")/max(" + maximum + ") values");
            }
            minimum = min;
            setValue(value);
        }

        void setValue(int value) {
            this.value = Math.min(Math.max(value, minimum), maximum);
        }

    }

    public static class CoSlider extends UIComponent<COHDEntryType_Slider> {


        private GraphicsElement<? extends GraphicsNode> refTrackNode;

        int minimum = 0;
        int maximum = 100;
        int value = 0;
        private GraphicsElement<? extends GraphicsNode> geKnob;
        private GraphicsElement<? extends GraphicsNode> geFill;
        private GraphicsElement<? extends GraphicsNode> geTrack;

        public CoSlider(final int index) {
            super(index);
        }

        @Override
        public void load(final ULD_File_Renderer renderer, final GraphicsElement<? extends GraphicsNode> parent, final COHDEntry entry, final SparseArray<COHDEntry> components, final COHDEntryType_Slider typeData) {
            super.load(renderer, parent, entry, components, typeData);
            geFill = getElement(typeData.refFill);
            if (geFill == null){
                Utils.getGlobalLogger().trace("geFillがnull");
            }
            @SuppressWarnings("unused")
            GraphicsElement<? extends GraphicsNode> geValueBox = getElement(typeData.refValueBox);
            refTrackNode = getElement(typeData.refTrack);
            geTrack = refTrackNode;
            geTrack.visible = false;
            GraphicsElement<? extends GraphicsNode> refKnobNode = getElement(typeData.refKnob);
            geKnob = refKnobNode;

            refTrackNode.height = parent.height;

            ((GraphicsComponent) refKnobNode).component.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    int height = refTrackNode.drawn.getBounds().height;
                    int y = refTrackNode.drawn.getBounds().y;
                    int q = e.getY() - y;
                    float p = (float) q / (float) height;
                    int vals = maximum - minimum + 1;
                    setValue((int) ((float) vals * p));
                }
            });

            if (parent.height > 0) {
                updateSize(parent);
            }
            setValue(value);
        }

        void updateSize(GraphicsElement<? extends GraphicsNode> all) {
            geTrack.height = all.height;
        }

        @SuppressWarnings("unused")
        public void setLimits(int min, int max) {
            if (min > max) {
                throw new RuntimeException("Invalid min(" + min + ")/max(" + max + ") values");
            }
            minimum = min;
            maximum = max;
            setValue(value);
        }

        public void setMax(int max) {
            if (minimum > max) {
                throw new RuntimeException("Invalid min(" + minimum + ")/max(" + max + ") values");
            }
            setValue(value);
        }

        public void setMin(int min) {
            if (min > maximum) {
                throw new RuntimeException("Invalid min(" + min + ")/max(" + maximum + ") values");
            }
            minimum = min;
            setValue(value);
        }

        private void setValue(int value) {
            this.value = Math.min(Math.max(value, minimum), maximum);
            float percent = (float)value / (maximum - minimum + 1);
            int f = (int) ((float) parent.height * (1 - percent));
            if (geFill != null){
                geFill.top = f;
                geFill.height = parent.height - f;
            }
            geKnob.top = f;
        }
    }
}
