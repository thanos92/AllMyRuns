package dv606.gc222bz.finalproject;

import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import dv606.gc222bz.finalproject.utilities.Costants;
import dv606.gc222bz.finalproject.utilities.PreferenceHelper;
import dv606.gc222bz.finalproject.utilities.Utilities;

public class SettingsActivity extends AppCompatActivity {

    EditTextPreference pref;
    PrefsFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        fragment = new PrefsFragment();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                fragment).commit();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);





    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        pref = (EditTextPreference)fragment.findPreference(getString(R.string.prefs_weight));
        pref.setSummary(pref.getText());

        pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {


            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {

                String textValue = (String) value;

                if(!TextUtils.isEmpty(textValue.trim())){
                    int numericValue = Integer.parseInt(textValue);

                    if(numericValue >= Costants.MIN_WEIGHT && numericValue <= Costants.MAX_WEIGHT){
                        pref.setSummary(String.format(getString(R.string.weight_with_unit_text), textValue));
                        return true;
                    }
                }

                Utilities.makeWelcomedialog(SettingsActivity.this, String.format(getString(R.string.error_weight_message),Costants.MIN_WEIGHT, Costants.MAX_WEIGHT), getString(R.string.error_message)).show();

                return false;
            }
        });

    }

    public static class PrefsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.settings_screen);
        }
    }

}