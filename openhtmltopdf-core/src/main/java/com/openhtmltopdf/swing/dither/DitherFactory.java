package com.openhtmltopdf.swing.dither;

import com.openhtmltopdf.swing.dither.strategy.LegacyDitherStrategy;
import com.openhtmltopdf.swing.dither.strategy.SimpleDitherStrategy;

import java.awt.image.BufferedImage;

import org.w3c.dom.Element;

/**
 * Dither strategy factory.
 * Provides global entry point to obtain dither strategy instance.
 * Supports runtime strategy selection and test mock injection via setInstance().
 */
public final class DitherFactory {

    private static BaseDither INSTANCE;

    private DitherFactory() {
        // utility class, prohibit instantiation
    }

    /**
     * Get global dither facade instance.
     * The facade will select concrete strategy according to html element attributes.
     * @return BaseDither facade implementation
     */
    public static BaseDither getDither() {
        if (INSTANCE == null) {
            INSTANCE = new DitherFacade();
        }
        return INSTANCE;
    }

    /**
     * Override global instance, mainly for unit test mock.
     * @param dither custom BaseDither implementation
     */
    public static void setInstance(BaseDither dither) {
        INSTANCE = dither;
    }

    /**
     * Select concrete strategy by element attribute.
     * <ul>
     * <li>if dither-kernel attribute exists: use SimpleDitherStrategy(new full-feature dither)</li>
     * <li>no dither-kernel: use LegacyDitherStrategy(for old template compatibility)</li>
     * </ul>
     * @param elem html img dom element
     * @return selected concrete BaseDither strategy
     */
    public static BaseDither selectStrategy(Element elem) {
        if (elem == null) {
            // return no-op strategy, return source image directly
            return (e, w, h, img) -> img;
        }
        String kernelAttr = elem.getAttribute(BaseDither.DITHER_KERNEL_ATTR);
        if (kernelAttr != null && !kernelAttr.trim().isEmpty()) {
            return new SimpleDitherStrategy();
        } else {
            return new LegacyDitherStrategy();
        }
    }

    /**
     * Internal facade implementation, delegate to selectStrategy.
     * Upper layer only calls facade.toImg(), no need to care about strategy switching.
     */
    private static class DitherFacade implements BaseDither {
        @Override
        public BufferedImage toImg(Element elem, int width, int height, BufferedImage newImg) {
            BaseDither realStrategy = DitherFactory.selectStrategy(elem);
            return realStrategy.toImg(elem, width, height, newImg);
        }
    }
}
