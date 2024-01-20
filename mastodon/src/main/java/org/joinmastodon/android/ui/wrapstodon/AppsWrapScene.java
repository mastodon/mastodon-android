package org.joinmastodon.android.ui.wrapstodon;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.model.AnnualReport;
import org.joinmastodon.android.ui.drawables.TiledDrawable;

import java.util.List;

import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class AppsWrapScene extends AnnualWrapScene{
	private final List<AnnualReport.NameAndCount> appStats;

	public AppsWrapScene(List<AnnualReport.NameAndCount> appStats){
		this.appStats=appStats;
	}

	@Override
	protected View onCreateContentView(Context context){
		LayoutInflater inflater=LayoutInflater.from(context);
		View content=inflater.inflate(R.layout.wrap_apps, null);
		content.setBackground(new TiledDrawable(context.getResources().getDrawable(R.drawable.chiclet_pattern, context.getTheme())));

		View[] appBars={
				content.findViewById(R.id.app1_bar),
				content.findViewById(R.id.app2_bar),
				content.findViewById(R.id.app3_bar)
		};
		ImageView[] appIcons={
				content.findViewById(R.id.app1_icon),
				content.findViewById(R.id.app2_icon),
				content.findViewById(R.id.app3_icon)
		};
		TextView[] appNames={
				content.findViewById(R.id.app1_name),
				content.findViewById(R.id.app2_name),
				content.findViewById(R.id.app3_name)
		};

		int max=appStats.get(0).count;
		int i=0;
		for(AnnualReport.NameAndCount app:appStats){
			appNames[i].setText(app.name);
			appBars[i].setTranslationY(V.dp(250)*(1-(app.count/(float)max)));
			if("Mastodon for Android".equals(app.name) || "Mastodon for iOS".equals(app.name)){
				appIcons[i].setImageResource(R.mipmap.ic_launcher);
			}else{
				String url=getIconUrl(app.name);
				if(url==null)
					url=getIconUrl(app.name.split(" ")[0]);
				if(url!=null){
					ViewImageLoader.loadWithoutAnimation(appIcons[i], null, new UrlImageLoaderRequest(Bitmap.Config.ARGB_8888, V.dp(96), V.dp(96), List.of(), Uri.parse(url)));
				}
			}
			i++;
			if(i==3)
				break;
		}
		if(i<3){
			for(int j=i;j<3;j++)
				appBars[j].setVisibility(View.INVISIBLE);
		}

		return content;
	}

	@Override
	protected void onDestroyContentView(){

	}

	static String getIconUrl(String name){
		return switch(name){
			case "Web" -> "https://upload.wikimedia.org/wikipedia/commons/thumb/0/08/Netscape_icon.svg/128px-Netscape_icon.svg.png";

			case "Rodent" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Frodent.d6a6e73b.png&w=1080&q=75";
			case "Focus" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ffocus.5d3f5755.png&w=1080&q=75";
			case "Tusky" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftusky.28fb3514.png&w=384&q=75";
			case "Subway Tooter" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fsubway-tooter.165ce486.png&w=384&q=75";
			case "Fedilab" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ffedilab.8fce088e.png&w=384&q=75";
			case "Megalodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmegalodon.d1fd421a.png&w=384&q=75";
			case "Moshidon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmoshidon.d0d53493.png&w=384&q=75";
			case "ZonePane" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fzonepane.add34ea8.png&w=1080&q=75";
			case "Pachli" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fpachli.0a3fda9d.png&w=1080&q=75";
			case "Toot!" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftoot.9fce2178.jpg&w=640&q=75";
			case "Mast" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmast.17193b21.png&w=640&q=75";
			case "iMast" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fimast_icon.9713d97a.png&w=1080&q=75";
			case "Ice Cubes" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ficecubes.141ad567.png&w=828&q=75";
			case "Ivory" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fivory.3878bb47.png&w=1080&q=75";
			case "Mammoth" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmammoth.19d0726e.png&w=828&q=75";
			case "Woolly" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fwoolly.270ab54a.png&w=1080&q=75";
			case "DAWN for Mastodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fdawn.5a728ea4.png&w=1080&q=75";
			case "Mona" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmona.962904ab.png&w=1080&q=75";
			case "Radiant" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fradiant.88eaaf27.png&w=1080&q=75";
			case "TootDesk" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftootdesk.089737e3.png&w=1080&q=75";
			case "Stomp (watchOS)" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fstomp.ac7757e7.png&w=2048&q=75";
			case "feather" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ffeather.448a23c4.png&w=2048&q=75";
			case "SoraSNS" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fsora.aa83aab1.png&w=1200&q=75";
			case "Pipilo" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fpipilo.9d0314d1.png&w=1080&q=75";
			case "Pinafore" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fpinafore.ba6b1933.png&w=256&q=75";
			case "Elk" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Felk.db28f01a.png&w=384&q=75";
			case "Buffer" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fbuffer.1b722f8e.png&w=1080&q=75";
			case "Statuzer" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fstatuzer.04c7fa8a.png&w=384&q=75";
			case "Fedica" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ffedica.a2b32162.png&w=2048&q=75";
			case "Phanpy" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fphanpy.97a4a3c1.png&w=384&q=75";
			case "Trunks" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftrunks.1e0e665e.png&w=1080&q=75";
			case "Litterbox" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Flitterbox.f4015748.png&w=640&q=75";
			case "Tooty" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftooty.f8664e1a.png&w=640&q=75";
			case "Mastodeck" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmastodeck.985ab21b.png&w=640&q=75";
			case "Tokodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftokodon.ba8f924d.png&w=640&q=75";
			case "Whalebird" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fwhalebird.cd50e388.png&w=256&q=75";
			case "TheDesk" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fthedesk.1cd41d27.png&w=1080&q=75";
			case "Hyper­space" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fhyperspace.668fa418.png&w=256&q=75";
			case "Mastonaut" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmastonaut.c026dca5.png&w=640&q=75";
			case "Sengi" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fsengi.c16fc152.png&w=828&q=75";
			case "Bitlbee-Mastodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fbitlbee.35ddf1a4.png&w=384&q=75";
			case "Tuba" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftuba.adee2feb.png&w=640&q=75";
			case "TootRain" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftootrain.34eff04b.png&w=640&q=75";
			case "Fedistar" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ffedistar.a2b814f8.png&w=256&q=75";
			case "Tooter" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Ftooter.1e9ff8f1.png&w=256&q=75";
			case "Amidon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Famidon.3578681d.png&w=256&q=75";
			case "BREXXTODON" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fbrexxtodon.c6174a1a.png&w=256&q=75";
			case "DOStodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fdostodon.cc63613e.png&w=256&q=75";
			case "Macstodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmacstodon.a47d72c0.png&w=256&q=75";
			case "Masto9" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmastonine.2f86e9a0.png&w=256&q=75";
			case "Mastodon for Apple II" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmastodonforappleii.6b9ba8ef.png&w=256&q=75";
			case "Mastodon 3.11 for Workgroups" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmastodonforworkgroups.c6524eb1.png&w=256&q=75";
			case "Heffalump" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fheffalump.b421f5e9.png&w=256&q=75";
			case "MOStodon" -> "https://joinmastodon.org/_next/image?url=%2F_next%2Fstatic%2Fmedia%2Fmostodon.bb1cb01c.png&w=3840&q=75";
			default -> null;
		};
	}
}
