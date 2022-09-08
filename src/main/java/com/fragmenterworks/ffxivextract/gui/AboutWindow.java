package com.fragmenterworks.ffxivextract.gui;

import com.fragmenterworks.ffxivextract.Constants;
import com.fragmenterworks.ffxivextract.Strings;
import com.fragmenterworks.ffxivextract.helpers.Utils;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@SuppressWarnings("FieldCanBeLocal")
class AboutWindow extends JFrame {

    private final JPanel aboutWindow = new JPanel();
    private final JPanel container = new JPanel();
    private final FancyJLabel appname = new FancyJLabel(Constants.APPNAME);
    private final JLabel author = new FancyJLabel("By Ioncannon");
    private final JLabel version = new FancyJLabel(Strings.ABOUTDIALOG_VERSION + " " + Constants.VERSION);
    private final JLabel gitcommit = new FancyJLabel(Strings.ABOUTDIALOG_GITVERSION + " " + Constants.COMMIT.substring(0, 10));
    private final JLabel website = new FancyJLabel("<html><a href=\"\">" + Constants.URL_WEBSITE + "</a></html>");
    private final JLabel specialThanks = new FancyJLabel("<html>Special Thanks to: Anwyll, Hezkezl, Clorifex, <br>and all those who donated to my tipjar!<br>ULD Renderer by Roze</html>");

    private final JLabel meImage = new JLabel();

    private int easterEggActivate = Constants.EASTER_EGG;

    private Font titleFont;
    private Font standardFont;

    private final JFrame parent;

    public AboutWindow(JFrame parent) {
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setSize(480, 700);
        this.setTitle(Strings.DIALOG_TITLE_ABOUT + " " + Constants.APPNAME);

        this.parent = parent;

        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        titleFont = new Font("ＭＳ Ｐゴシック", Font.BOLD, 20);
        standardFont = new Font("ＭＳ Ｐゴシック", Font.PLAIN, 14);

        appname.setFont(titleFont);
        author.setFont(standardFont);
        version.setFont(standardFont);
        gitcommit.setFont(standardFont);
        website.setFont(standardFont);
        specialThanks.setFont(standardFont);

        ImageIcon image = new ImageIcon(Objects.requireNonNull(getClass().getResource("/me.png")));
        meImage.setIcon(image);

        website.setCursor(new Cursor(Cursor.HAND_CURSOR));
        goWebsite(website);

        container.add(appname);
        container.add(author);
        container.add(version);
        container.add(gitcommit);
        container.add(website);
        container.add(specialThanks);

        container.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel centerPanel = new JPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        centerPanel.setLayout(new GridBagLayout());
        centerPanel.add(container, gbc);

        aboutWindow.setLayout(new BorderLayout());
        aboutWindow.add(centerPanel, BorderLayout.LINE_START);
        aboutWindow.add(meImage, BorderLayout.LINE_END);
        getContentPane().add(aboutWindow);

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/frameicon.png")));

        this.setIconImage(icon.getImage());
        this.pack();
        this.setSize(getWidth(), getHeight() - 10);
        this.setResizable(false);

        //イースターエッグ
        meImage.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
            }

            @Override
            public void mouseClicked(MouseEvent arg0) {
                easterEggActivate++;

                if (easterEggActivate >= 5) {
                    try {
                        InputStream fontStream = Objects.requireNonNull(getClass().getResource("/cache")).openStream();
                        //エオルゼア文字に変更
                        Constants.setUIFont(new FontUIResource(Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(13.5f)));

                        titleFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(20.0f);
                        standardFont = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(14.0f);

                        appname.setFont(titleFont);
                        author.setFont(standardFont);
                        version.setFont(standardFont);
                        gitcommit.setFont(standardFont);

                        SwingUtilities.updateComponentTreeUI(AboutWindow.this.parent);

                    } catch (FontFormatException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void goWebsite(JLabel website) {
        website.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(Constants.URL_WEBSITE));
                } catch (IOException ex) {
                    //問題があるようです
                } catch (URISyntaxException e1) {
                    Utils.getGlobalLogger().error(e1);
                }
            }
        });
    }

    private static class FancyJLabel extends JLabel {

        FancyJLabel(String string) {
            super(string);
        }

        @Override
        public void paintComponent(Graphics g) {
            Graphics2D graphics2d = (Graphics2D) g;
            graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            super.paintComponent(g);
        }

    }

}

