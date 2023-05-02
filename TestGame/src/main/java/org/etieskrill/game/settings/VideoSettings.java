package org.etieskrill.game.settings;

public class VideoSettings {
    
    private WindowMode windowMode;
    private long primaryMonitor;
    private WindowSize size;
    private boolean vSync;
    private float targetFrameRate;
    private float uiScale;
    
    public enum WindowMode {
        FULLSCREEN,
        BORDERLESS,
        WINDOWED
    }
    
    public enum WindowSize {
        HD(720, 1280),
        FHD(1080, 1920),
        WQHD(1440, 2560),
        UHD(2160, 3840),
        VGA(480, 640),
        SVGA(600, 800),
        XGA(768, 1024),
        UWHD(1080, 2560),
        UWQHD(1440, 3440);
        
        WindowSize(final int width, final int height) {
            if (width > height)
                throw new IllegalArgumentException("Width should not be larger than height");
            
            this.width = width;
            this.height = height;
        }
    
        final float width;
        final float height;
        
        public final float getAspectRatio() {
            return width / height;
        }
    }
    
}
