package com.openhtmltopdf.swing.dither;

import org.w3c.dom.Element;

import com.openhtmltopdf.swing.dither.strategy.LegacyDitherStrategy;
import com.openhtmltopdf.swing.dither.strategy.SimpleDitherStrategy;

/**
 * 策略选择器
 * 根据DOM标签属性，返回对应的抖动策略实现
 */
public final class DitherStrategySelector {
	private DitherStrategySelector(){}

	/**
	 * @param elem img dom元素
	 * @return BaseDither 具体策略实例
	 */
	public static BaseDither select(Element elem) {
		if(elem == null){
			return (e,w,h,img) -> img;
		}
		String kernelAttr = elem.getAttribute(BaseDither.DITHER_KERNEL_ATTR);
		if(kernelAttr != null && !kernelAttr.trim().isEmpty()){
			return new SimpleDitherStrategy();
		}else{
			
			return new LegacyDitherStrategy();
		}
	}
}
