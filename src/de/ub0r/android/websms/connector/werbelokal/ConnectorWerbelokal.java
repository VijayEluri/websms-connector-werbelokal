/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of WebSMS.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.websms.connector.werbelokal;

import java.util.ArrayList;

import org.apache.http.message.BasicNameValuePair;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.ub0r.android.websms.connector.common.BasicConnector;
import de.ub0r.android.websms.connector.common.ConnectorCommand;
import de.ub0r.android.websms.connector.common.ConnectorSpec;
import de.ub0r.android.websms.connector.common.Log;
import de.ub0r.android.websms.connector.common.Utils;
import de.ub0r.android.websms.connector.common.WebSMSException;

/**
 * AsyncTask to manage IO to werbelokal.de API.
 * 
 * @author flx
 */
public final class ConnectorWerbelokal extends BasicConnector {
	/** Tag for output. */
	private static final String TAG = "werbelokal";
	/** SubConnectorSpec ID: with sender. */
	private static final String ID_W_SENDER = "2";
	/** SubConnectorSpec ID: without sender. */
	private static final String ID_WO_SENDER = "1";

	/** CherrySMS Gateway URL - send. */
	private static final String URL_SEND = // .
	"http://www.werbelokal.de/websmsdroid.html";
	/** CherrySMS Gateway URL - check. */
	private static final String URL_CHECK = // .
	"http://www.werbelokal.de/websmsdroid_konto.html";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectorSpec initSpec(final Context context) {
		final String name = context
				.getString(R.string.connector_connector_name);
		ConnectorSpec c = new ConnectorSpec(name);
		c.setAuthor(// .
				context.getString(R.string.connector_connector_author));
		c.setBalance(null);
		c.setCapabilities(ConnectorSpec.CAPABILITIES_UPDATE
				| ConnectorSpec.CAPABILITIES_SEND
				| ConnectorSpec.CAPABILITIES_PREFS);
		c.addSubConnector(ID_WO_SENDER, context.getString(R.string.wo_sender),
				0);
		c.addSubConnector(ID_W_SENDER, context.getString(R.string.w_sender), 0);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ConnectorSpec updateSpec(final Context context,
			final ConnectorSpec connectorSpec) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		if (p.getBoolean(Preferences.PREFS_ENABLED, false)) {
			if (p.getString(Preferences.PREFS_USERNAME, "").length() > 0 && // .
					p.getString(Preferences.PREFS_PASSWORD, "").length() > 0) {
				connectorSpec.setReady();
			} else {
				connectorSpec.setStatus(ConnectorSpec.STATUS_ENABLED);
			}
		} else {
			connectorSpec.setStatus(ConnectorSpec.STATUS_INACTIVE);
		}
		return connectorSpec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamUsername() {
		return "email";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamPassword() {
		return "password";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamSubconnector() {
		return "tarif";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamRecipients() {
		return "destination";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamSender() {
		return "xxx";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getParamText() {
		return "text";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getUsername(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		return p.getString(Preferences.PREFS_USERNAME, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getPassword(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		return p.getString(Preferences.PREFS_PASSWORD, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getRecipients(final ConnectorCommand command) {
		String[] recipients = Utils.national2international(command
				.getDefPrefix(), command.getRecipients());
		return recipients[0].substring(1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getSender(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs) {
		return "xxx";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getUrlBalance(final ArrayList<BasicNameValuePair> d) {
		return URL_CHECK;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getUrlSend(final ArrayList<BasicNameValuePair> d) {
		return URL_SEND;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean usePost() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void parseResponse(final Context context,
			final ConnectorCommand command, final ConnectorSpec cs,
			final String htmlText) throws WebSMSException {
		if (htmlText == null || htmlText.length() == 0) {
			throw new WebSMSException(context, R.string.error_service);
		}
		final String[] lines = htmlText.split("\n");
		final String line0 = lines[0].replaceAll("<br>", "").trim();
		if (command.getType() == ConnectorCommand.TYPE_SEND) {
			if (line0.indexOf("SMS wurde versandt.") >= 0) {
				Log.d(TAG, line0);
			} else {
				throw new WebSMSException(line0);
			}
		} else {
			if (line0.indexOf("Guthaben: ") >= 0) {
				cs.setBalance(line0.split(" ")[1] + "\u20AC");
			} else {
				throw new WebSMSException(line0);
				// throw new WebSMSException("could not parse ret");
			}
		}
	}
}
