package org.joinmastodon.android.ui;

import org.joinmastodon.android.model.Attachment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;

public class PhotoLayoutHelper{
	public static final int MAX_WIDTH=1000;
	public static final int MAX_HEIGHT=1777; // 9:16
	public static final int MIN_HEIGHT=563;

	@NonNull
	public static TiledLayoutResult processThumbs(List<Attachment> thumbs){
		int _maxW=MAX_WIDTH;
		int _maxH=MAX_HEIGHT;

		TiledLayoutResult result=new TiledLayoutResult();
		if(thumbs.size()==1){
			Attachment att=thumbs.get(0);
			result.rowSizes=result.columnSizes=new int[]{1};
			if(att.getWidth()>att.getHeight()){
				result.width=_maxW;
				result.height=Math.max(MIN_HEIGHT, Math.round(att.getHeight()/(float)att.getWidth()*_maxW));
			}else{
				result.height=_maxH;
				result.width=Math.round(att.getWidth()/(float)att.getHeight()*_maxH);
			}
			result.tiles=new TiledLayoutResult.Tile[]{new TiledLayoutResult.Tile(1, 1, result.width, result.height, 0, 0)};
			return result;
		}else if(thumbs.size()==0){
			throw new IllegalArgumentException("Empty thumbs array");
		}

		String orients="";
		ArrayList<Float> ratios=new ArrayList<Float>();
		int cnt=thumbs.size();


		for(Attachment thumb : thumbs){
			float ratio=thumb.getWidth()/(float) thumb.getHeight();
			char orient=ratio>1.2 ? 'w' : (ratio<0.8 ? 'n' : 'q');
			orients+=orient;
			ratios.add(ratio);
		}

		float avgRatio=!ratios.isEmpty() ? sum(ratios)/ratios.size() : 1.0f;

		float maxW, maxH, marginW=0, marginH=0;
		maxW=_maxW;
		maxH=_maxH;

		float maxRatio=maxW/maxH;

		if(cnt==2){
			if(orients.equals("ww") && avgRatio>1.4*maxRatio && (ratios.get(1)-ratios.get(0))<0.2){ // two wide photos, one above the other
				float h=Math.max(Math.min(maxW/ratios.get(0), Math.min(maxW/ratios.get(1), (maxH-marginH)/2.0f)), MIN_HEIGHT/2f);

				result.width=Math.round(maxW);
				result.height=Math.round(h*2+marginH);
				result.columnSizes=new int[]{result.width};
				result.rowSizes=new int[]{Math.round(h), Math.round(h)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(1, 1, maxW, h, 0, 0),
						new TiledLayoutResult.Tile(1, 1, maxW, h, 0, 1)
				};
			}else if(orients.equals("ww") || orients.equals("qq")){ // next to each other, same ratio
				float w=((maxW-marginW)/2);
				float h=Math.max(Math.min(w/ratios.get(0), Math.min(w/ratios.get(1), maxH)), MIN_HEIGHT);

				result.width=Math.round(maxW);
				result.height=Math.round(h);
				result.columnSizes=new int[]{Math.round(w), _maxW-Math.round(w)};
				result.rowSizes=new int[]{Math.round(h)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(1, 1, w, h, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w, h, 1, 0)
				};
			}else{ // next to each other, different ratios
				float w0=((maxW-marginW)/ratios.get(1)/(1/ratios.get(0)+1/ratios.get(1)));
				float w1=(maxW-w0-marginW);
				float h=Math.max(Math.min(maxH, Math.min(w0/ratios.get(0), w1/ratios.get(1))), MIN_HEIGHT);

				result.columnSizes=new int[]{Math.round(w0), Math.round(w1)};
				result.rowSizes=new int[]{Math.round(h)};
				result.width=Math.round(w0+w1+marginW);
				result.height=Math.round(h);
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(1, 1, w0, h, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w1, h, 1, 0)
				};
			}
		}else if(cnt==3){
			if((ratios.get(0) > 1.2 * maxRatio || avgRatio > 1.5 * maxRatio) || orients.equals("www")){ // 2nd and 3rd photos are on the next line
				float hCover=Math.min(maxW/ratios.get(0), (maxH-marginH)*0.66f);
				float w2=((maxW-marginW)/2);
				float h=Math.min(maxH-hCover-marginH, Math.min(w2/ratios.get(1), w2/ratios.get(2)));
				if(hCover+h<MIN_HEIGHT){
					float prevTotalHeight=hCover+h;
					hCover=MIN_HEIGHT*(hCover/prevTotalHeight);
					h=MIN_HEIGHT*(h/prevTotalHeight);
				}
				result.width=Math.round(maxW);
				result.height=Math.round(hCover+h+marginH);
				result.columnSizes=new int[]{Math.round(w2), _maxW-Math.round(w2)};
				result.rowSizes=new int[]{Math.round(hCover), Math.round(h)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(2, 1, maxW, hCover, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w2, h, 0, 1),
						new TiledLayoutResult.Tile(1, 1, w2, h, 1, 1)
				};
			}else{ // 2nd and 3rd photos are on the right part
				float wCover=Math.min(maxH*ratios.get(0), (maxW-marginW)*0.75f);
				float h1=(ratios.get(1)*(maxH-marginH)/(ratios.get(2)+ratios.get(1)));
				float h0=(maxH-h1-marginH);
				float w=Math.min(maxW-wCover-marginW, Math.min(h1*ratios.get(2), h0*ratios.get(1)));
				result.width=Math.round(wCover+w+marginW);
				result.height=Math.round(maxH);
				result.columnSizes=new int[]{Math.round(wCover), Math.round(w)};
				result.rowSizes=new int[]{Math.round(h0), Math.round(h1)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(1, 2, wCover, maxH, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w, h0, 1, 0),
						new TiledLayoutResult.Tile(1, 1, w, h1, 1, 1)
				};
			}
		}else if(cnt==4){
			if((ratios.get(0) > 1.2 * maxRatio || avgRatio > 1.5 * maxRatio) || orients.equals("wwww")){ // 2nd, 3rd and 4th photos are on the next line
				float hCover=Math.min(maxW/ratios.get(0), (maxH-marginH)*0.66f);
				float h=(maxW-2*marginW)/(ratios.get(1)+ratios.get(2)+ratios.get(3));
				float w0=h*ratios.get(1);
				float w1=h*ratios.get(2);
				float w2=h*ratios.get(3);
				h=Math.min(maxH-hCover-marginH, h);
				if(hCover+h<MIN_HEIGHT){
					float prevTotalHeight=hCover+h;
					hCover=MIN_HEIGHT*(hCover/prevTotalHeight);
					h=MIN_HEIGHT*(h/prevTotalHeight);
				}
				result.width=Math.round(maxW);
				result.height=Math.round(hCover+h+marginH);
				result.columnSizes=new int[]{Math.round(w0), Math.round(w1), _maxW-Math.round(w0)-Math.round(w1)};
				result.rowSizes=new int[]{Math.round(hCover), Math.round(h)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(3, 1, maxW, hCover, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w0, h, 0, 1),
						new TiledLayoutResult.Tile(1, 1, w1, h, 1, 1),
						new TiledLayoutResult.Tile(1, 1, w2, h, 2, 1),
				};
			}else{ // 2nd, 3rd and 4th photos are on the right part
				float wCover= Math.min(maxH*ratios.get(0), (maxW-marginW)*0.66f);
				float w=(maxH-2*marginH)/(1/ratios.get(1)+1/ratios.get(2)+1/ratios.get(3));
				float h0=w/ratios.get(1);
				float h1=w/ratios.get(2);
				float h2=w/ratios.get(3)+marginH;
				w=Math.min(maxW-wCover-marginW, w);
				result.width=Math.round(wCover+marginW+w);
				result.height=Math.round(maxH);
				result.columnSizes=new int[]{Math.round(wCover), Math.round(w)};
				result.rowSizes=new int[]{Math.round(h0), Math.round(h1), Math.round(h2)};
				result.tiles=new TiledLayoutResult.Tile[]{
						new TiledLayoutResult.Tile(1, 3, wCover, maxH, 0, 0),
						new TiledLayoutResult.Tile(1, 1, w, h0, 1, 0),
						new TiledLayoutResult.Tile(1, 1, w, h1, 1, 1),
						new TiledLayoutResult.Tile(1, 1, w, h2, 1, 2),
				};
			}
		}else{
			ArrayList<Float> ratiosCropped=new ArrayList<Float>();
			if(avgRatio>1.1){
				for(float ratio : ratios){
					ratiosCropped.add(Math.max(1.0f, ratio));
				}
			}else{
				for(float ratio : ratios){
					ratiosCropped.add(Math.min(1.0f, ratio));
				}
			}

			HashMap<int[], float[]> tries=new HashMap<>();

			// One line
			int firstLine, secondLine, thirdLine;
			tries.put(new int[]{firstLine=cnt}, new float[]{calculateMultiThumbsHeight(ratiosCropped, maxW, marginW)});

			// Two lines
			for(firstLine=1; firstLine<=cnt-1; firstLine++){
				tries.put(new int[]{firstLine, secondLine=cnt-firstLine}, new float[]{
								calculateMultiThumbsHeight(ratiosCropped.subList(0, firstLine), maxW, marginW),
								calculateMultiThumbsHeight(ratiosCropped.subList(firstLine, ratiosCropped.size()), maxW, marginW)
						}
				);
			}

			// Three lines
			for(firstLine=1; firstLine<=cnt-2; firstLine++){
				for(secondLine=1; secondLine<=cnt-firstLine-1; secondLine++){
					tries.put(new int[]{firstLine, secondLine, thirdLine=cnt-firstLine-secondLine}, new float[]{
									calculateMultiThumbsHeight(ratiosCropped.subList(0, firstLine), maxW, marginW),
									calculateMultiThumbsHeight(ratiosCropped.subList(firstLine, firstLine+secondLine), maxW, marginW),
									calculateMultiThumbsHeight(ratiosCropped.subList(firstLine+secondLine, ratiosCropped.size()), maxW, marginW)
							}
					);
				}
			}

			// Looking for minimum difference between thumbs block height and maxH (may probably be little over)
			int[] optConf=null;
			float optDiff=0;
			for(int[] conf : tries.keySet()){
				float[] heights=tries.get(conf);
				float confH=marginH*(heights.length-1);
				for(float h : heights) confH+=h;
				float confDiff=Math.abs(confH-maxH);
				if(conf.length>1){
					if(conf[0]>conf[1] || conf.length>2 && conf[1]>conf[2]){
						confDiff*=1.1;
					}
				}
				if(optConf==null || confDiff<optDiff){
					optConf=conf;
					optDiff=confDiff;
				}
			}

			ArrayList<Attachment> thumbsRemain=new ArrayList<>(thumbs);
			ArrayList<Float> ratiosRemain=new ArrayList<>(ratiosCropped);
			float[] optHeights=tries.get(optConf);
			int k=0;

			result.width=Math.round(maxW);
			result.rowSizes=new int[optHeights.length];
			result.tiles=new TiledLayoutResult.Tile[thumbs.size()];
			float totalHeight=0f;
			ArrayList<Integer> gridLineOffsets=new ArrayList<>();
			ArrayList<ArrayList<TiledLayoutResult.Tile>> rowTiles=new ArrayList<>(optHeights.length);

			for(int i=0; i<optConf.length; i++){
				int lineChunksNum=optConf[i];
				ArrayList<Attachment> lineThumbs=new ArrayList<>();
				for(int j=0; j<lineChunksNum; j++) lineThumbs.add(thumbsRemain.remove(0));
				float lineHeight=optHeights[i];
				totalHeight+=lineHeight;
				result.rowSizes[i]=Math.round(lineHeight);
				int totalWidth=0;
				ArrayList<TiledLayoutResult.Tile> row=new ArrayList<>();
				for(int j=0; j<lineThumbs.size(); j++){
					float thumb_ratio=ratiosRemain.remove(0);
					float w=j==lineThumbs.size()-1 ? (maxW-totalWidth) : (thumb_ratio*lineHeight);
					totalWidth+=Math.round(w);
					if(j<lineThumbs.size()-1 && !gridLineOffsets.contains(totalWidth))
						gridLineOffsets.add(totalWidth);
					TiledLayoutResult.Tile tile=new TiledLayoutResult.Tile(1, 1, w, lineHeight, 0, i);
					result.tiles[k]=tile;
					row.add(tile);
					k++;
				}
				rowTiles.add(row);
			}
			Collections.sort(gridLineOffsets);
			gridLineOffsets.add(Math.round(maxW));
			result.columnSizes=new int[gridLineOffsets.size()];
			result.columnSizes[0]=gridLineOffsets.get(0);
			for(int i=gridLineOffsets.size()-1; i>0; i--){
				result.columnSizes[i]=gridLineOffsets.get(i)-gridLineOffsets.get(i-1);
			}

			for(ArrayList<TiledLayoutResult.Tile> row : rowTiles){
				int columnOffset=0;
				for(TiledLayoutResult.Tile tile : row){
					int startColumn=columnOffset;
					tile.startCol=startColumn;
					int width=0;
					tile.colSpan=0;
					for(int i=startColumn; i<result.columnSizes.length; i++){
						width+=result.columnSizes[i];
						tile.colSpan++;
						if(width==tile.width){
							break;
						}
					}
					columnOffset+=tile.colSpan;
				}
			}
			result.height=Math.round(totalHeight+marginH*(optHeights.length-1));
		}

		return result;
	}

	private static float sum(List<Float> a){
		float sum=0;
		for(float f:a) sum+=f;
		return sum;
	}

	private static float calculateMultiThumbsHeight(List<Float> ratios, float width, float margin){
		return (width-(ratios.size()-1)*margin)/sum(ratios);
	}


	public static class TiledLayoutResult{
		public int[] columnSizes, rowSizes; // sizes in grid fractions
		public Tile[] tiles;
		public int width, height; // in pixels (510x510 max)

		@Override
		public String toString(){
			return "TiledLayoutResult{"+
					"columnSizes="+Arrays.toString(columnSizes)+
					", rowSizes="+Arrays.toString(rowSizes)+
					", tiles="+Arrays.toString(tiles)+
					", width="+width+
					", height="+height+
					'}';
		}

		public static class Tile{
			public int colSpan, rowSpan, width, height, startCol, startRow;

			public Tile(int colSpan, int rowSpan, int width, int height, int startCol, int startRow){
				this.colSpan=colSpan;
				this.rowSpan=rowSpan;
				this.width=width;
				this.height=height;
				this.startCol=startCol;
				this.startRow=startRow;
			}

			public Tile(int colSpan, int rowSpan, float width, float height, int startCol, int startRow){
				this(colSpan, rowSpan, Math.round(width), Math.round(height), startCol, startRow);
			}

			@Override
			public String toString(){
				return "Tile{"+
						"colSpan="+colSpan+
						", rowSpan="+rowSpan+
						", width="+width+
						", height="+height+
						'}';
			}
		}
	}
}
