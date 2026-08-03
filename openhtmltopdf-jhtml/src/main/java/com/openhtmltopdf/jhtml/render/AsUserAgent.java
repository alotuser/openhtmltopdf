package com.openhtmltopdf.jhtml.render;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import com.openhtmltopdf.java2d.Java2DUserAgent;
import com.openhtmltopdf.java2d.image.AWTFSImage;
import com.openhtmltopdf.jhtml.util.OkHttpUtil;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
import com.openhtmltopdf.outputdevice.helper.ExternalResourceType;
import com.openhtmltopdf.resource.ImageResource;
import com.openhtmltopdf.util.LogMessageId;
import com.openhtmltopdf.util.XRLog;
/**
 * Java2DUserAgent is not used directly because we need to override getImageResource() to support OkHttpUtil for http/https resources.
 */
public class AsUserAgent extends Java2DUserAgent {

	@Override
	public ImageResource getImageResource(String uri, ExternalResourceType type) {

		ImageResource ir;

		if (!checkAccessAllowed(uri, type, ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI)) {
			return null;
		}

		String resolved = _resolver.resolveURI(this._baseUri, uri);

		if (!checkAccessAllowed(resolved, type, ExternalResourceControlPriority.RUN_AFTER_RESOLVING_URI)) {
			return null;
		}

		if (resolved == null) {
			XRLog.log(Level.INFO, LogMessageId.LogMessageId2Param.LOAD_URI_RESOLVER_REJECTED_LOADING_AT_URI, "image resource", uri);
			return null;
		}

		// First, we check the internal per run cache.
		ir = _imageCache.get(resolved);
		if (ir != null) {
			return ir;
		}
		if (!OkHttpUtil.isHttp(resolved)) {
			return super.getImageResource(uri, type);
		} else {
			// Finally we fetch from the network or file, etc.
			try (InputStream is = OkHttpUtil.getInputStream(uri)) {
				if (is != null) {

					BufferedImage img = ImageIO.read(is);

					if (img == null) {
						throw new IOException("ImageIO.read() returned null");
					}

					AWTFSImage fsImage2 = (AWTFSImage) AWTFSImage.createImage(img);

					ir = new ImageResource(resolved, fsImage2);
					_imageCache.put(resolved, ir);

					return ir;
				}
			} catch (FileNotFoundException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI_NOT_FOUND, resolved);
			} catch (IOException e) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI, uri, e);
			} catch (Exception e1) {
				XRLog.log(Level.WARNING, LogMessageId.LogMessageId1Param.EXCEPTION_CANT_READ_IMAGE_FILE_FOR_URI, uri, e1);
			}

			// Failed.
			return new ImageResource(resolved, null);
		}

	}

}
