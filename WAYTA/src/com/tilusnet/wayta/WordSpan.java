package com.tilusnet.wayta;

import android.graphics.Color;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

public class WordSpan extends ClickableSpan {

	private final int alpha;
	private int id;
	private TextPaint textpaint;
	public boolean shouldHilightWord = false;

	public WordSpan(int anID, int alpha) {
		id = anID;
		this.alpha = alpha;
		// if the word selected is the same as the ID set the highlight flag
	}

	@Override
	public void updateDrawState(TextPaint ds) {
		textpaint = ds;
		ds.setColor(Color.BLACK);
		textpaint.bgColor = Color.argb(alpha, 0, 200, 0);
		//Remove default underline associated with spans
		ds.setUnderlineText(false);

	}

	/*
	 * public void changeSpanBgColor(View widget){ shouldHilightWord = true;
	 * updateDrawState(textpaint); widget.invalidate();
	 * 
	 * }
	 * 
	 * @Override
	 */

	@Override
	public void onClick(View widget) {

		// TODO Auto-generated method stub

	}

	/**
	 * This function sets the span to record the word number, as the span ID
	 * 
	 * @param spanID
	 */
	public void setSpanTextID(int spanID) {
		id = spanID;
	}

	/**
	 * Return the wordId of this span
	 * 
	 * @return id
	 */
	public int getSpanTextID() {
		return id;
	}
}