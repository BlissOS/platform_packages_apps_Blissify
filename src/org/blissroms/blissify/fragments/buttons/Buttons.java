/*
 * Copyright (C) 2014-2021 The BlissRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.blissroms.blissify.fragments.buttons;

import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_2BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL_OVERLAY;

import android.content.ContentResolver;
import android.content.Context;
import android.content.om.IOverlayManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.ServiceManager;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import static com.android.systemui.shared.recents.utilities.Utilities.isLargeScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class Buttons extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "Buttons";

    private static final String KEY_ENABLE_TASKBAR = "enable_taskbar";

    private SwitchPreference mEnableTaskbar;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.blissify_button);
        final PreferenceScreen prefScreen = getPreferenceScreen();

        mEnableTaskbar = (SwitchPreference) findPreference(KEY_ENABLE_TASKBAR);
        if (mEnableTaskbar != null) {
            if (!hasNavigationBar()) {
                prefScreen.removePreference(mEnableTaskbar);
            } else {
                mEnableTaskbar.setOnPreferenceChangeListener(this);
                mEnableTaskbar.setChecked(Settings.System.getInt(getContentResolver(),
                        Settings.System.ENABLE_TASKBAR,
                        isLargeScreen(getContext()) ? 1 : 0) == 1);
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mEnableTaskbar) {
            boolean value = (Boolean) newValue;
            if (is2ButtonNavigationEnabled(getContext())) {
                // Let's switch to gestural mode if user previously had 2 buttons enabled.
                setButtonNavigationMode(NAV_BAR_MODE_GESTURAL_OVERLAY);}
            Settings.System.putInt(getContentResolver(),
                    Settings.System.ENABLE_TASKBAR, value ? 1 : 0);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.BLISSIFY;
    }

    private static boolean is2ButtonNavigationEnabled(Context context) {
        return NAV_BAR_MODE_2BUTTON == context.getResources().getInteger(
                com.android.internal.R.integer.config_navBarInteractionMode);
    }

    private static void setButtonNavigationMode(String overlayPackage) {
        IOverlayManager overlayManager = IOverlayManager.Stub.asInterface(
                ServiceManager.getService(Context.OVERLAY_SERVICE));
        try {
            overlayManager.setEnabledExclusiveInCategory(overlayPackage, UserHandle.USER_CURRENT);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private static boolean hasNavigationBar() {
        boolean hasNavigationBar = false;
        try {
            IWindowManager windowManager = WindowManagerGlobal.getWindowManagerService();
            hasNavigationBar = windowManager.hasNavigationBar(Display.DEFAULT_DISPLAY);
        } catch (RemoteException e) {
            Log.e(TAG, "Error getting navigation bar status");
        }
        return hasNavigationBar;
    }

    /**
     * For Search.
     */

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.blissify_button);
}
