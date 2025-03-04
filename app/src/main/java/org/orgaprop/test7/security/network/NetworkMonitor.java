package org.orgaprop.test7.security.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.util.Log;

public class NetworkMonitor {
	private static final int MIN_BANDWIDTH_KBPS = 1000;

	private final Context context;
	private ConnectivityManager.NetworkCallback networkCallback;
	private NetworkStateListener networkStateListener;

	public interface NetworkStateListener {
		void onNetworkLost();

		void onNetworkAvailable();

		void onNetworkSpeedChanged(long speedKbps);
	}

	public NetworkMonitor(Context context) {
		this.context = context.getApplicationContext();
	}

	public void setNetworkStateListener(NetworkStateListener listener) {
		this.networkStateListener = listener;
	}

	public void startMonitoring() {
		if (networkCallback != null)
			return;

		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		networkCallback = createNetworkCallback();
		cm.registerDefaultNetworkCallback(networkCallback);
	}

	public void stopMonitoring() {
		if (networkCallback == null)
			return;

		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			cm.unregisterNetworkCallback(networkCallback);
			networkCallback = null;
		} catch (IllegalArgumentException e) {
			Log.e("NetworkMonitor","Erreur lors de l'arrêt du monitoring réseau", e);
		}
	}

	private ConnectivityManager.NetworkCallback createNetworkCallback() {
		return new ConnectivityManager.NetworkCallback() {
			@Override
			public void onLost(Network network) {
				Log.w("NetworkMonitor", "Connexion réseau perdue");
				if (networkStateListener != null) {
					networkStateListener.onNetworkLost();
				}
			}

			@Override
			public void onAvailable(Network network) {
				Log.i("NetworkMonitor", "Connexion réseau disponible");
				if (networkStateListener != null) {
					networkStateListener.onNetworkAvailable();
				}
			}

			@Override
			public void onCapabilitiesChanged(Network network, NetworkCapabilities capabilities) {
				int bandwidth = capabilities.getLinkDownstreamBandwidthKbps();
				if (bandwidth < MIN_BANDWIDTH_KBPS) {
					Log.w("NetworkMonitor", "Connexion lente détectée: {}Kbps"+bandwidth);
				}
				if (networkStateListener != null) {
					networkStateListener.onNetworkSpeedChanged(bandwidth);
				}
			}
		};
	}

	public boolean checkConnectivity() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		Network network = cm.getActiveNetwork();
		if (network == null)
			return false;

		NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
		return capabilities != null &&
				(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
						capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
	}

	public long checkNetworkSpeed() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
		return capabilities != null ? capabilities.getLinkDownstreamBandwidthKbps() : 0;
	}
}
