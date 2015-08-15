package com.nestapi.codelab.demo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nestapi.lib.API.*;
import com.nestapi.lib.AuthManager;
import com.nestapi.lib.ClientMetadata;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends ActionBarActivity implements
        View.OnClickListener,
        NestAPI.AuthenticationListener,
        Listener.StructureListener,
        Listener.ThermostatListener, AdapterView.OnItemClickListener,
        AdapterView.OnItemSelectedListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String THERMOSTAT_KEY = "thermostat_key";
    private static final String STRUCTURE_KEY = "structure_key";

    private static final int AUTH_TOKEN_REQUEST_CODE = 101;

    private ActionBarDrawerToggle drawerToggle;
    private DrawerLayout drawerLayout;
    private RelativeLayout leftLayout;
    private ListView drawerList;
    private Spinner structureSpinner;

    private TextView mAmbientTempText;
    private View mSingleControlContainer;
    private TextView mCurrentTempText;
    private View mThermostatView;
    private View mRangeControlContainer;
    private TextView mCurrentCoolTempText;
    private TextView mCurrentHeatTempText;
    private Button mStructureAway;

    private Listener mUpdateListener;
    private NestAPI mNestApi;
    private AccessToken mToken;
    private Thermostat mThermostat;
    private Structure mStructure;

    private CommandQueue commandQueue;

    private long mCurrentTargetTempF;
    private long mPreviousTargetTempF;

	private double mCurrentTargetTempC;
    private double mPreviousTargetTempC;

    private ArrayList<Thermostat> thermostatList;
    private ArrayList<Structure> structureList;

    private ArrayList<String> thermostatNamesList;
    private ArrayList<String> structureNamesList;

    private ArrayAdapter<String> thermostatAdapter;
    private ArrayAdapter<String> structureAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thermostatList = new ArrayList<Thermostat>();
        structureList = new ArrayList<Structure>();

        thermostatNamesList = new ArrayList<String>();
        structureNamesList = new ArrayList<String>();

        mThermostatView = findViewById(R.id.thermostat_view);
        mSingleControlContainer = findViewById(R.id.single_control);
        mCurrentTempText = (TextView)findViewById(R.id.current_temp);
        mAmbientTempText = (TextView)findViewById(R.id.ambient_temp);
        mStructureAway = (Button)findViewById(R.id.structure_away_btn);
        mRangeControlContainer = findViewById(R.id.range_control);
        mCurrentCoolTempText = (TextView)findViewById(R.id.current_cool_temp);
        mCurrentHeatTempText = (TextView)findViewById(R.id.current_heat_temp);
        drawerList= (ListView)findViewById(R.id.drawer_list);
        drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        leftLayout = (RelativeLayout)findViewById(R.id.left_drawer);
        structureSpinner = (Spinner)findViewById(R.id.structure_spinner);

        mStructureAway.setOnClickListener(mStructureAwayClickListener);
        findViewById(R.id.heat).setOnClickListener(mModeClickListener);
        findViewById(R.id.cool).setOnClickListener(mModeClickListener);
        findViewById(R.id.heat_cool).setOnClickListener(mModeClickListener);
        findViewById(R.id.off).setOnClickListener(mModeClickListener);
        findViewById(R.id.temp_up).setOnClickListener(this);
        findViewById(R.id.temp_down).setOnClickListener(this);
        findViewById(R.id.temp_cool_up).setOnClickListener(this);
        findViewById(R.id.temp_cool_down).setOnClickListener(this);
        findViewById(R.id.temp_heat_up).setOnClickListener(this);
        findViewById(R.id.temp_heat_down).setOnClickListener(this);

        mNestApi = NestAPI.getInstance();
        mToken = Settings.loadAuthToken(this);
        if (mToken != null) {
            authenticate(mToken);
        } else {
            obtainAccessToken();
        }

        if (savedInstanceState != null) {
            mThermostat = savedInstanceState.getParcelable(THERMOSTAT_KEY);
            mStructure = savedInstanceState.getParcelable(STRUCTURE_KEY);
            updateView();
        }

        if(getResources().getBoolean(R.bool.portrait_only)){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        initializeDrawer();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(THERMOSTAT_KEY, mThermostat);
        outState.putParcelable(STRUCTURE_KEY, mStructure);
    }

    @Override
    public void onRestoreInstanceState (Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mThermostat = savedInstanceState.getParcelable(THERMOSTAT_KEY);
            mStructure = savedInstanceState.getParcelable(STRUCTURE_KEY);
            updateView();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
//        mNestApi.removeUpdateListener(mUpdateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return drawerToggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view

        return super.onPrepareOptionsMenu(menu);
    }

    private void obtainAccessToken() {
        Log.v(TAG, "starting auth flow...");
        final ClientMetadata metadata = new ClientMetadata.Builder()
                .setClientID(Constants.CLIENT_ID)
                .setClientSecret(Constants.CLIENT_SECRET)
                .setRedirectURL(Constants.REDIRECT_URL)
                .build();
        AuthManager.launchAuthFlow(this, AUTH_TOKEN_REQUEST_CODE, metadata);
    }

    private void initializeDrawer() {

        // set a custom shadow that overlays the main content when the drawer opens
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if (mThermostat != null) {
                    getSupportActionBar().setTitle(mThermostat.getName());
                }
                else {
                    getSupportActionBar().setTitle(mStructure.getName());
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()

                updateView();
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);

                getSupportActionBar().setTitle("Your homes");
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(drawerToggle);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
    }

    private View.OnClickListener mStructureAwayClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mStructure == null) {
                return;
            }

            Structure.AwayState awayState;

            switch (mStructure.getAwayState()) {
                case AUTO_AWAY:
                case AWAY:
                    awayState = Structure.AwayState.HOME;
                    mStructureAway.setText(R.string.away_state_home);
                    break;
                case HOME:
                    awayState = Structure.AwayState.AWAY;
                    mStructureAway.setText(R.string.away_state_away);
                    break;
                default:
                    return;
            }

            mNestApi.setStructureAway(mStructure.getStructureID(), awayState, new NestAPI.CompletionListener() {
                @Override
                public void onComplete() {
                    Log.i(TAG, "Done setting to AWAY");
                }

                @Override
                public void onError(int errorCode, String message) {
                    Log.i(TAG, String.format("Error setting to away: %s", message));
					Toast.makeText(getApplicationContext(), String.format("Error setting to away: %s", message), Toast.LENGTH_SHORT).show();
                }
            });
        }
    };

    private View.OnClickListener mModeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mThermostat == null) {
                return;
            }

            final String thermostatID = mThermostat.getDeviceID();
            final Thermostat.HVACMode mode;

            switch (v.getId()) {
                case R.id.heat:
                    mode = Thermostat.HVACMode.HEAT;
                    break;
                case R.id.cool:
                    mode = Thermostat.HVACMode.COOL;
                    break;
                case R.id.heat_cool:
                    mode = Thermostat.HVACMode.HEAT_AND_COOL;
                    break;
                case R.id.off:
                default:
                    mode = Thermostat.HVACMode.OFF;
                    break;
            }

            mNestApi.setHVACMode(thermostatID, mode, null);
        }
    };

    @Override
    public void onClick(View v) {
        if (mThermostat == null) {
            return;
        }

        switch (mThermostat.getHVACmode()) {
            case HEAT_AND_COOL:
                updateTempRange(v);
                break;
            case OFF:
                //NO-OP
                break;
            default:
                updateTempSingle(v);
                break;
        }
    }

    private void updateOff() {
        mCurrentTempText.setText(R.string.thermostat_off);
        mThermostatView.setBackgroundResource(R.drawable.off_thermostat_drawable);
    }

    private void updateTempRange(View v) {
        String thermostatID = mThermostat.getDeviceID();
		double difference;
		long tempHighF = 0;
		long tempLowF = 0;
		double tempHighC = 0;
		double tempLowC = 0;

		if (mThermostat.getTemperatureScale().equals( "F" )) {
			difference = 3;
			tempHighF = mThermostat.getTargetTemperatureHighF();
			tempLowF = mThermostat.getTargetTemperatureLowF();
		}
		else {
			difference = 1.5;
			tempHighC = mThermostat.getTargetTemperatureHighC();
			tempLowC = mThermostat.getTargetTemperatureLowC();
		}

        switch (v.getId()) {
            case R.id.temp_cool_down:
                if (mThermostat.getTemperatureScale().equals( "F" )) {
					tempLowF -= 1;
					if (restrictRange(difference, tempHighF, tempLowF)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 3 degrees F.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureLowF(thermostatID, tempLowF, null);
					mCurrentCoolTempText.setText(Long.toString(tempLowF));
				}
				else {
					tempLowC -= 0.5;
					if (restrictRange(difference, tempHighC, tempLowC)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 1.5 degrees C.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureLowC(thermostatID, tempLowC, null);
					mCurrentCoolTempText.setText(Double.toString(tempLowC));
				}
                break;
            case R.id.temp_cool_up:
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					tempLowF += 1;
					if (restrictRange(difference, tempHighF, tempLowF)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 3 degrees F.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureLowF(thermostatID, tempLowF, null);
					mCurrentCoolTempText.setText(Long.toString(tempLowF));
				}
				else {
					tempLowC += 0.5;
					if (restrictRange(difference, tempHighC, tempLowC)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 1.5 degrees C.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureLowC(thermostatID, tempLowC, null);
					mCurrentCoolTempText.setText(Double.toString(tempLowC));
				}
				break;
            case R.id.temp_heat_down:
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					tempHighF -= 1;
					if (restrictRange(difference, tempHighF, tempLowF)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 3 degrees F.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureHighF(thermostatID, tempHighF, null);
					mCurrentHeatTempText.setText(Long.toString(tempHighF));
				}
				else {
					tempHighC -= 0.5;
					if (restrictRange(difference, tempHighC, tempLowC)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 1.5 degrees C.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureHighC(thermostatID, tempHighC, null);
					mCurrentHeatTempText.setText(Double.toString(tempHighC));
				}
				break;
            case R.id.temp_heat_up:
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					tempHighF += 1;
					if (restrictRange(difference, tempHighF, tempLowF)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 3 degrees F.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureHighF(thermostatID, tempHighF, null);
					mCurrentHeatTempText.setText(Long.toString(tempHighF));
				}
				else {
					tempHighC += 0.5;
					if (restrictRange(difference, tempHighC, tempLowC)) {
						Toast.makeText(getApplicationContext(), "Cannot set target temperature closer than 1.5 degrees C.", Toast.LENGTH_SHORT).show();
						return;
					}
					mNestApi.setTargetTemperatureHighC(thermostatID, tempHighC, null);
					mCurrentHeatTempText.setText(Double.toString(tempHighC));
				}
				break;
        }
    }

	private boolean restrictRange(double difference, double tempHigh, double tempLow) {
		if ( tempHigh - tempLow < difference) {
			return true;
        }
		return false;
	}

	private void updateTempSingle(View v) {

        if(mStructure.getAwayState() == Structure.AwayState.AWAY) {
            Toast.makeText(getApplicationContext(), "Cannot change target temperature while structure is away", Toast.LENGTH_LONG).show();
            return;
        }

        mPreviousTargetTempF = mCurrentTargetTempF;
        mPreviousTargetTempC = mCurrentTargetTempC;
        switch (v.getId()) {
            case R.id.temp_up:
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					mCurrentTargetTempF += 1;
					if (restrictSingle(mCurrentTargetTempF, mCurrentTargetTempC)) {
						Toast.makeText(getApplicationContext(), "Temperature F value is too high.", Toast.LENGTH_SHORT).show();
						mCurrentTargetTempF -= 1;
						return;
					}
				}
				else {
					mCurrentTargetTempC += 0.5;
					if (restrictSingle(mCurrentTargetTempF, mCurrentTargetTempC)) {
						Toast.makeText(getApplicationContext(), "Temperature C value is too high.", Toast.LENGTH_SHORT).show();
						mCurrentTargetTempC -= 0.5;
						return;
					}
				}
                break;
            case R.id.temp_down:
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					mCurrentTargetTempF -= 1;
					if (restrictSingle(mCurrentTargetTempF, mCurrentTargetTempC)) {
						Toast.makeText(getApplicationContext(), "Temperature F value is too low.", Toast.LENGTH_SHORT).show();
						mCurrentTargetTempF += 1;
						return;
					}
				}
				else {
					mCurrentTargetTempC -= 0.5;
					if (restrictSingle(mCurrentTargetTempF, mCurrentTargetTempC)) {
						Toast.makeText(getApplicationContext(), "Temperature C value is too low.", Toast.LENGTH_SHORT).show();
						mCurrentTargetTempC += 0.5;
						return;
					}
				}
                break;
        }

		if (mThermostat.getTemperatureScale().equals( "F" )) {
			mCurrentTempText.setText(Long.toString(mCurrentTargetTempF));
		}
        else {
			mCurrentTempText.setText(Double.toString(mCurrentTargetTempC));
		}

        getQueue();

        //Now add a new command to the queue
        if (commandQueue != null) {

            if(!commandQueue.acceptsCommands()) {
                Toast.makeText(getApplicationContext(), "Limit reached. Please try again later.", Toast.LENGTH_SHORT).show();
				if (mThermostat.getTemperatureScale().equals( "F" )) {
					mCurrentTargetTempF = mPreviousTargetTempF;
					mCurrentTempText.setText(Long.toString(mCurrentTargetTempF));
				}
				else {
					mCurrentTargetTempC = mPreviousTargetTempC;
					mCurrentTempText.setText(Double.toString(mCurrentTargetTempC));
				}
                return;
            }
            commandQueue.addCommand(new ThermostatCommand() {
				@Override
				public void run() {
					if (mThermostat.getTemperatureScale().equals("F")) {
						mNestApi.setTargetTemperatureF(mThermostat.getDeviceID(), mCurrentTargetTempF, this);
					} else {
						mNestApi.setTargetTemperatureC(mThermostat.getDeviceID(), mCurrentTargetTempC, this);
					}
				}

				@Override
				public void onError(int errorCode, String message) {
					if (message.equals("Too many requests")) { // Too many requests
						commandQueue.setLimit_reached(true);
						commandQueue.setLimit_reached_date(new Date());
						Toast.makeText(getApplicationContext(), "You have reached Nest's limits for this hour. Try again later.", Toast.LENGTH_LONG).show();
					} else if (message.equals("Temperature F value too high for lock temperature.")) {
						Toast.makeText(getApplicationContext(), "Temperature F value too high for lock temperature.", Toast.LENGTH_LONG).show();
					} else if (message.equals("Temperature F value too low for lock temperature.")) {
						Toast.makeText(getApplicationContext(), "Temperature F value too low for lock temperature.", Toast.LENGTH_LONG).show();
					}

					if (mThermostat.getTemperatureScale().equals("F")) {
						mCurrentTargetTempF = mPreviousTargetTempF;
						mCurrentTempText.setText(Long.toString(mCurrentTargetTempF));
					}
					else {
						mCurrentTargetTempC = mPreviousTargetTempC;
						mCurrentTempText.setText(Double.toString(mCurrentTargetTempC));
					}
				}

				@Override
				public void onComplete() {
					if (mThermostat.getTemperatureScale().equals("F")) {
						mCurrentTargetTempF = mThermostat.getTargetTemperatureF();
					}
					else {
						mCurrentTargetTempC = mThermostat.getTargetTemperatureC();
					}
					updateView();
//                    Toast.makeText(getApplicationContext(), String.format("Set temp to: %d", mCurrentTargetTempF), Toast.LENGTH_LONG).show();
				}
			});
        }

        saveQueue();
    }

	private boolean restrictSingle(long tempF, double tempC) {
		if (tempF < 50 || tempF > 90) {
			return true;
		}
		else if (tempC < 9 || tempC > 32) {
			return true;
		}
		return false;
	}

    private void updateView() {
        updateAmbientTempTextView();
        updateStructureViews();
        updateThermostatViews();
    }

    private void updateAmbientTempTextView() {
        if (mThermostat != null) {
			if (mThermostat.getTemperatureScale().equals( "F" )) {
				mAmbientTempText.setText(Long.toString(mThermostat.getAmbientTemperatureF()));
			}
			else {
				mAmbientTempText.setText(Double.toString(mThermostat.getAmbientTemperatureC()));
			}
        }
    }

    private void updateStructureViews() {
        if (mStructure != null) {
            mStructureAway.setText(mStructure.getAwayState().getKey());
        }
    }

    private void updateThermostatViews() {
		if (mStructure != null && mStructure.getThermostatCount() == 0) {
			mCurrentTempText.setText(R.string.empty_structure);
			mThermostatView.setBackgroundResource(R.drawable.empty_structure_drawable);
			return;
		}

        if (mThermostat == null || mStructure == null) {
            return;
        }

        Thermostat.HVACMode mode = mThermostat.getHVACmode();
        int singleControlVisibility;
        int rangeControlVisibility;
        Structure.AwayState state = mStructure.getAwayState();
        boolean isAway = state == Structure.AwayState.AWAY || state == Structure.AwayState.AUTO_AWAY;

        if(isAway) {
            mSingleControlContainer.setVisibility(View.VISIBLE);
            mRangeControlContainer.setVisibility(View.GONE);
            updateSingleControlView();
            return;
        }

        switch (mode) {
            case HEAT_AND_COOL:
                singleControlVisibility = View.GONE;
                rangeControlVisibility = View.VISIBLE;
                updateRangeControlView();
                break;
            case OFF:
                singleControlVisibility = View.VISIBLE;
                rangeControlVisibility = View.GONE;
                updateOff();
                break;
            default:
                singleControlVisibility = View.VISIBLE;
                rangeControlVisibility = View.GONE;
                updateSingleControlView();
                break;
        }

        mSingleControlContainer.setVisibility(singleControlVisibility);
        mRangeControlContainer.setVisibility(rangeControlVisibility);
    }

    private void updateRangeControlView() {
		final double tempDiffHigh;
		final double tempDiffLow;

		if (mThermostat.getTemperatureScale().equals( "F" )) {
			mCurrentHeatTempText.setText(Long.toString(mThermostat.getTargetTemperatureHighF()));
			mCurrentCoolTempText.setText(Long.toString(mThermostat.getTargetTemperatureLowF()));

			tempDiffHigh = mThermostat.getTargetTemperatureHighF() - mThermostat.getAmbientTemperatureF();
			tempDiffLow = mThermostat.getTargetTemperatureLowF() - mThermostat.getAmbientTemperatureF();
		}
		else {
			mCurrentHeatTempText.setText(Double.toString(mThermostat.getTargetTemperatureHighC()));
			mCurrentCoolTempText.setText(Double.toString(mThermostat.getTargetTemperatureLowC()));

			tempDiffHigh = mThermostat.getTargetTemperatureHighC() - mThermostat.getAmbientTemperatureC();
			tempDiffLow = mThermostat.getTargetTemperatureLowC() - mThermostat.getAmbientTemperatureC();
		}

        final int thermostatDrawable;
        if (tempDiffHigh < 0) {
            thermostatDrawable = R.drawable.cool_thermostat_drawable;
        } else if(tempDiffLow > 0) {
            thermostatDrawable = R.drawable.heat_thermostat_drawable;
        } else {
            thermostatDrawable = R.drawable.off_thermostat_drawable;
        }
        mThermostatView.setBackgroundResource(thermostatDrawable);
    }

    private void updateSingleControlView() {

		Structure.AwayState state = mStructure.getAwayState();
        if(state == Structure.AwayState.AWAY || state == Structure.AwayState.AUTO_AWAY) {
            mCurrentTempText.setText(R.string.thermostat_away);
            mThermostatView.setBackgroundResource(R.drawable.off_thermostat_drawable);
            return;
        }

		long mCurrentTemperatureF;
		double mCurrentTemperatureC;
		final double tempDiffF;
		if (mThermostat.getTemperatureScale().equals( "F" )) {
			mCurrentTemperatureF = mThermostat.getTargetTemperatureF();

			Log.v(TAG, "targetTempF: " + mThermostat.getTargetTemperatureF() + " ambientF: " + mThermostat.getAmbientTemperatureF());

			tempDiffF = mThermostat.getTargetTemperatureF() - mThermostat.getAmbientTemperatureF();
			mCurrentTempText.setText(Long.toString(mCurrentTemperatureF));
		}
		else {
			mCurrentTemperatureC = mThermostat.getTargetTemperatureC();

			Log.v(TAG, "targetTempC: " + mThermostat.getTargetTemperatureC() + " ambientC: " + mThermostat.getAmbientTemperatureC());

			tempDiffF = mThermostat.getTargetTemperatureC() - mThermostat.getAmbientTemperatureC();
			mCurrentTempText.setText(Double.toString(mCurrentTemperatureC));
		}


        final int thermostatDrawable;
        switch (mThermostat.getHVACmode()) {
            case HEAT:
                thermostatDrawable = tempDiffF > 0 ? R.drawable.heat_thermostat_drawable : R.drawable.off_thermostat_drawable;
                break;
            case COOL:
                thermostatDrawable = tempDiffF < 0 ? R.drawable.cool_thermostat_drawable : R.drawable.off_thermostat_drawable;
                break;
            case OFF:
            default:
                thermostatDrawable = R.drawable.off_thermostat_drawable;
                break;
        }
        mThermostatView.setBackgroundResource(thermostatDrawable);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK || requestCode != AUTH_TOKEN_REQUEST_CODE) {
            return;
        }

        if (AuthManager.hasAccessToken(data)) {
            mToken = AuthManager.getAccessToken(data);
            Settings.saveAuthToken(this, mToken);
            Log.v(TAG, "Main Activity parsed auth token: " + mToken.getToken() + " expires: " + mToken.getExpiresIn());
            authenticate(mToken);
        } else {
            Log.e(TAG, "Unable to resolve access token from payload.");
        }
    }

    private void authenticate(AccessToken token) {
        Log.v(TAG, "Authenticating...");
        NestAPI.getInstance().authenticate(token, this);
    }

    @Override
    public void onAuthenticationSuccess() {
        Log.v(TAG, "Authentication succeeded.");
        fetchData();
    }

    @Override
    public void onAuthenticationFailure(int errorCode) {
        Log.v(TAG, "Authentication failed with error: " + errorCode);
		if ( errorCode == -999) { // when token has expired
			Settings.clearAuthToken(this);
			obtainAccessToken();
		}
		else if ( errorCode == -7) { // when user manually removed Works with nest connection
			Settings.clearAuthToken(this);
			obtainAccessToken();
		}
		else if ( errorCode == -5) { // The active or pending auth credentials were superseded by another call to auth
			// What do here?
			return;
		}
    }

    private void fetchData(){
        Log.v(TAG, "Fetching data...");

        mUpdateListener = new Listener.Builder()
                .setStructureListener(this)
                .setThermostatListener(this)
                .build();

        mNestApi.addUpdateListener(mUpdateListener);
    }


    // ******************************************************************************************
    // Handle nest updates

    private void setupThermostatAdapter() {
        // Setup drawer as soon as the first thermostat becomes available
        if ( drawerList.getAdapter() == null ) {
            thermostatAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item, thermostatNamesList);

            drawerList.setAdapter(thermostatAdapter);
            // Set the list's click listener
            drawerList.setOnItemClickListener(this);
        }
    }

    private void setupStructureAdapter() {
        // Setup drawer as soon as the first structure becomes available
        if ( structureSpinner.getAdapter() == null ) {
            structureAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, structureNamesList);

            structureAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            structureSpinner.setAdapter(structureAdapter);
            structureSpinner.setOnItemSelectedListener(this);
        }
    }

    private void updateAdapters() {
        if(thermostatAdapter != null) {
            thermostatAdapter.notifyDataSetChanged();
        }
        if(structureAdapter != null) {
            structureAdapter.notifyDataSetChanged();
        }
    }

    private void updateStructureNames() {
        structureNamesList.clear();
        for(Structure s:structureList){
            structureNamesList.add(s.getName());
        }
    }

    private void updateThermostatNames() {
        if (mStructure == null) {
            return;
        }

        // Refresh the drawer list
        boolean found = false;
        thermostatNamesList.clear();
        for (Thermostat thermostat: thermostatList) {
            if (thermostat.getStructureID().equals(mStructure.getStructureID())) {
                thermostatNamesList.add(thermostat.getName());
                found = true;
            }
        }
        if(!found) {
            thermostatNamesList.add("(empty home)");
        }
        updateAdapters();
    }

    private void updateStructureList(Structure structure) {
        boolean found = false;
        for(int i = 0; i < structureList.size(); i++) {
            if (structureList.get(i).getStructureID().equals(structure.getStructureID())) {
                structureList.set(i, structure);
                found = true;
                updateAdapters();
                break;
            }
        }
        if (!found) {
            structureList.add(structure);
            updateAdapters();
        }
    }

    private void updateCurrentStructure(Structure structure) {
        if (mStructure == null) {
            mStructure = structure;
        }
        // Update the current structure if the update was for it
        if (structure.getStructureID().equals(mStructure.getStructureID())) {
            mStructure = structure;
            updateView();
        }
    }

    private void updateCurrentThermostat(Thermostat thermostat) {
        // Initialize the current thermostat
        if (mThermostat == null) {
            mThermostat = thermostat;
        }
        if(mThermostat.getDeviceID().equals(thermostat.getDeviceID())) {
            mThermostat = thermostat;
            getSupportActionBar().setTitle(thermostat.getName());
            updateView();
        }
    }

    private void updateThermostatList(Thermostat thermostat) {
        boolean found = false;
        for(int i = 0; i < thermostatList.size(); i++) {
            if (thermostatList.get(i).getDeviceID().equals(thermostat.getDeviceID())) {
                thermostatList.set(i, thermostat);
                updateThermostatNames();
                updateAdapters();
                found = true;
                break;
            }
        }
        if (!found) {
            thermostatList.add(thermostat);
            updateThermostatNames();
            updateAdapters();
        }
    }

    @Override
    public void onStructureUpdated(Structure structure) {
        Log.i(TAG, String.format("Structure (%s) updated.", structure.getName()));
        Log.i(TAG, String.format("### %s went to %s", structure.getName(), structure.getAwayState().toString()));

        updateStructureList(structure);
        updateCurrentStructure(structure);
        updateStructureNames();
        setupStructureAdapter();
    }

    @Override
    public void onThermostatUpdated(Thermostat thermostat) {
        Log.i(TAG, String.format("Thermostat (%s) updated.", thermostat.getName()));

        updateThermostatList(thermostat);
        updateCurrentThermostat(thermostat);
        setupThermostatAdapter();
    }


    // ******************************************************************************************
    // Spinner item click
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        // Set the default structure
        mStructure = structureList.get(position);

        // Rebuild the list of thermostats for this structure
        updateThermostatNames();

        if( mStructure.getThermostatIDs().size() != 0) {

            // Set the first thermostat in the structure as default
            for (Thermostat thermostat : thermostatList) {
                if (thermostat.getStructureID().equals(mStructure.getStructureID())) {
                    mThermostat = thermostat;
                    mCurrentTargetTempF = mThermostat.getTargetTemperatureF();
					mCurrentTargetTempC = mThermostat.getTargetTemperatureC();
                    getSupportActionBar().setTitle(mThermostat.getName());
                    updateView();
                    break;
                }
            }
        }
        else {
            mThermostat = null;
            getSupportActionBar().setTitle("Empty structure");
            updateView();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // NOP
    }

    // ******************************************************************************************
    // Drawer item click
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        TextView textView = (TextView)view;
        for (int j = 0; j < thermostatList.size(); j++ ) {
            if (thermostatList.get(j).getName().equals(textView.getText())) {
                mThermostat = thermostatList.get(j);
                mCurrentTargetTempF = mThermostat.getTargetTemperatureF();
                getSupportActionBar().setTitle(mThermostat.getName());
                updateView();
                break;
            }
        }

        storeLastThermostat();

        drawerLayout.closeDrawer(leftLayout);
    }

    private void storeLastThermostat() {
        if(mThermostat == null) return;

        SharedPreferences sharedPref = getSharedPreferences("apppreferences", Context.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("last_thermostat_id", mThermostat.getDeviceID());
        editor.apply();
    }

    // ******************************************************************************************
    // Get / Save Command Queue
    private void getQueue() {
        if(commandQueue == null) {
            commandQueue = CommandQueue.readQueue(getApplicationContext(), mThermostat.getDeviceID());
        }
    }

    private void saveQueue() {
        if(commandQueue != null) {
            CommandQueue.writeQueue(getApplicationContext(), commandQueue);
        }
    }

}
