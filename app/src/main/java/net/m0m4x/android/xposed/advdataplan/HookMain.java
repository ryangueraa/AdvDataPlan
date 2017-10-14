package net.m0m4x.android.xposed.advdataplan;
/**
 * Created by max on 09/04/2017.
 */

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XModuleResources;
import android.os.Build;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static java.lang.Math.abs;


public class HookMain implements IXposedHookZygoteInit, IXposedHookInitPackageResources, IXposedHookLoadPackage {

    private static final boolean DEBUG = true;

    /****************************
        RESOURCES Hooking

     */

    private static String MODULE_PATH = null;

    static int R_layout_data_usage_cycle_editor;
    static int R_id_datepicker;
    static int R_id_cycle_days;
    static int R_id_cycle_day;

    static int modR_strings_dataplan_days;
    static int modR_strings_dataplan_day;
    static int modR_strings_nr1_daily;
    static int modR_strings_nr7_weekly;
    static int modR_strings_nr30_fixedmonth;
    static int modR_strings_nr31_monthly;

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        MODULE_PATH = startupParam.modulePath;
        if(DEBUG) XposedBridge.log("HOOK init AdvDataPlan - modulePath:" + startupParam.modulePath + " sdk: "+Build.VERSION.SDK_INT+"");

    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        if(DEBUG) XposedBridge.log("HOOK RES init -  " + resparam.packageName + " ! ");
        if(!resparam.packageName.equals("com.android.settings")) {
            return;
        }

        /*
            Get ID of module resources

            Marshmallow: native
            Nougat: Ok!
         */
        XModuleResources modRes = XModuleResources.createInstance(MODULE_PATH, resparam.res);
        modR_strings_dataplan_days = resparam.res.addResource(modRes, R.string.dataplan_days);
        modR_strings_dataplan_day = resparam.res.addResource(modRes, R.string.dataplan_day);
        modR_strings_nr1_daily = resparam.res.addResource(modRes, R.string.nr1_daily);
        modR_strings_nr7_weekly = resparam.res.addResource(modRes, R.string.nr7_weekly);
        modR_strings_nr30_fixedmonth = resparam.res.addResource(modRes, R.string.nr30_fixedmonth);
        modR_strings_nr31_monthly = resparam.res.addResource(modRes, R.string.nr31_monthly);

        /*
            Get ID of native resources

            Marshmallow: native
            Nougat: Ok!
         */
        R_layout_data_usage_cycle_editor = resparam.res.getIdentifier("data_usage_cycle_editor", "layout", "com.android.settings");
        if(DEBUG) XposedBridge.log("HOOK RES       ...found R.layout.data_usage_cycle_editor : " + R_layout_data_usage_cycle_editor + " !");

        /*
            Hook Layout 'data_usage_cycle_editor'

            Marshmallow: native
            Nougat: Ok!
        */
        resparam.res.hookLayout("com.android.settings", "layout", "data_usage_cycle_editor", new XC_LayoutInflated() {
            @SuppressLint("NewApi")
            @Override
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                if(DEBUG) XposedBridge.log("HOOK RES layout is inflating... - data_usage_cycle_editor!");

                Context context = liparam.view.getContext();

                /*
                layout 0 : [
                            txtview     <= original    (visibility GONE)
                            numpicker   <= original    (visibility GONE)
                            + layout 1     [    datepicker    ]
                            + layout 2     [txtview  numpicker]
                           ]
                 */
                int i150dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, context.getResources().getDisplayMetrics());
                int i100dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, context.getResources().getDisplayMetrics());
                int i48dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, context.getResources().getDisplayMetrics());
                int i16dip = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());

                try {

                    //numberPicker
                    R_id_cycle_day = liparam.res.getIdentifier("cycle_day", "id", "com.android.settings");
                    NumberPicker l0_num =  (NumberPicker) liparam.view.findViewById(R_id_cycle_day);
                    l0_num.setVisibility(View.GONE);

                    //debug
                    //if (DEBUG) view_dump(l0_num);

                    //layout 0 (root - ViewGroup)
                    ViewGroup res_layout0 = (ViewGroup) l0_num.getParent();
                    if(DEBUG) XposedBridge.log("         Parent layout is " + res_layout0.getClass().getName());
                    //case: LinearLayout - set Vertical
                    if (res_layout0 instanceof LinearLayout) {
                        if(DEBUG) XposedBridge.log("                          LinearLayout instance detected.");
                        LinearLayout res_lin_layout0 = (LinearLayout) res_layout0;
                        res_lin_layout0.setOrientation(LinearLayout.VERTICAL);
                    }

                    //hide originals layouts
                    try {
                        for (int i = 0; i < res_layout0.getChildCount(); i++) {
                            res_layout0.getChildAt(i).setVisibility(View.GONE);
                        }
                    } catch(Exception e) {
                        if(DEBUG) XposedBridge.log("Warning - Cannot empty layout!");
                        e.printStackTrace();
                    }

                    //layout 1
                    LinearLayout res_layout1 = new LinearLayout(context);
                    res_layout1.setOrientation(LinearLayout.HORIZONTAL);
                    TextView l1_txt = new TextView(context);
                    LinearLayout.LayoutParams l1_txt_lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    l1_txt_lp.gravity = Gravity.CENTER_VERTICAL;
                    l1_txt_lp.setMarginStart(i16dip);
                    l1_txt.setLayoutParams(l1_txt_lp);
                    l1_txt.setGravity(Gravity.CENTER_VERTICAL);
                    //l2_txt.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
                    l1_txt.setText(modR_strings_dataplan_day);
                    DatePickerDialog l1_dat = new DatePickerDialog(context, android.R.style.Theme_Holo_Light_Dialog, null, 2017,04,16);
                    l1_dat.getDatePicker().findViewById(context.getResources().getIdentifier("year","id","android")).setVisibility(View.GONE);
                    l1_dat.getDatePicker().setId(View.generateViewId());
                    R_id_datepicker = l1_dat.getDatePicker().getId();
                    l1_dat.getDatePicker().setLayoutParams(new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
                    l1_dat.getDatePicker().setCalendarViewShown(false);
                    l1_dat.getDatePicker().setSpinnersShown(true);
                    //l1_dat.getDatePicker().setVisibility(View.GONE);
                    res_layout1.addView(l1_txt);
                    res_layout1.addView(l1_dat.getDatePicker());

                    //layout 2
                    LinearLayout res_layout2 = new LinearLayout(context);
                    res_layout2.setOrientation(LinearLayout.HORIZONTAL);
                    TextView l2_txt = new TextView(context);
                    LinearLayout.LayoutParams l2_txt_lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
                    l2_txt_lp.gravity = Gravity.CENTER_VERTICAL;
                    l2_txt_lp.setMarginStart(i16dip);
                    l2_txt.setLayoutParams(l2_txt_lp);
                    l2_txt.setGravity(Gravity.CENTER_VERTICAL);
                    //l2_txt.setTextAppearance(context, android.R.style.TextAppearance_DeviceDefault_Medium);
                    l2_txt.setText(modR_strings_dataplan_days);
                    NumberPicker l2_num = new NumberPicker(context);
                    l2_num.setId(View.generateViewId());
                    R_id_cycle_days = l2_num.getId();
                    LinearLayout.LayoutParams l2_num_lp = new LinearLayout.LayoutParams( i150dip, i100dip);
                    l2_num_lp.setMarginEnd(i16dip);
                    l2_num_lp.setMarginStart(i16dip);
                    l2_num.setLayoutParams(l2_num_lp);
                    l2_num.setGravity(Gravity.CENTER_VERTICAL);
                    res_layout2.addView(l2_txt);
                    res_layout2.addView(l2_num);

                    //Adding Layouts
                    (res_layout0).addView(res_layout1);
                    (res_layout0).addView(res_layout2);

                } catch (Exception e) {
                    XposedBridge.log("HOOK RES layout Exception! ");
                    e.printStackTrace();
                }

                if(DEBUG) XposedBridge.log("HOOK RES layout is inflated!");
            }
        });

    }





    /****************************
        METHODS Hooking

     */

    public static final int CYCLE_NONE = -1;
    private static final String EXTRA_TEMPLATE = "template";

    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if(DEBUG) XposedBridge.log("HOOK handleLoadPackage - " + lpparam.packageName + "! " );

        /*
            NetworkPolicy Classes - Compute cycle Boundaries

            Marshmallow: native
            Nougat: Ok!
         */
        if(         lpparam.packageName.equals("android")
                ||  lpparam.packageName.equals("com.android.systemui")
                ||  lpparam.packageName.equals("com.android.settings")
                ||  lpparam.packageName.equals("com.android.providers.settings")
                ){
            if(DEBUG) XposedBridge.log("HOOK NetworkPolicyManager methods! (pkg:"+lpparam.packageName+")");

            final Class<?> NetworkPolicyManager = XposedHelpers.findClass(
                    "android.net.NetworkPolicyManager",
                    lpparam.classLoader);
            final Class<?> NetworkPolicy = XposedHelpers.findClass(
                    "android.net.NetworkPolicy",
                    lpparam.classLoader);

            findAndHookMethod(NetworkPolicyManager, "computeNextCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if(DEBUG) XposedBridge.log("HOOK android.net > computeNextCycleBoundary !!! (pkg:"+lpparam.packageName+")");

                    // Get Params
                    long currentTime = (long) param.args[0];    // long currentTime
                    Object policy = param.args[1];              // NetworkPolicy policy
                    int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                    // Check
                    if (cycle_day == CYCLE_NONE) {
                        throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                    }

                    return mComputeNextCycleBoundary(currentTime, cycle_day);

                }

            });

            findAndHookMethod(NetworkPolicyManager, "computeLastCycleBoundary", long.class , NetworkPolicy , new XC_MethodReplacement() {

                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    if(DEBUG) XposedBridge.log("HOOK  android.net > computeLastCycleBoundary (pkg:"+lpparam.packageName+")");

                    // Get Params
                    long currentTime = (long) param.args[0];    // long currentTime
                    Object policy = param.args[1];              // NetworkPolicy policy
                    int cycle_day = (int) XposedHelpers.getObjectField(policy, "cycleDay");

                    //TODO Check policy template mobile, otherwise do default

                    // Check
                    if (cycle_day == CYCLE_NONE) {
                        throw new IllegalArgumentException("Unable to compute boundary without cycleDay");
                    }
                    return mComputeLastCycleBoundary(currentTime, cycle_day);

                }

            });

        }

        /*
            DataUsage Classes - Editor Dialog Fragment

            Marshmallow: native
            Nougat:  Ok!

         */
        if( lpparam.packageName.equals("com.android.settings") ) {
                if(DEBUG) XposedBridge.log("HOOK DataUsageSummary methods! (pkg:"+lpparam.packageName+")!");

            //SDK23
            if (Build.VERSION.SDK_INT == 23) {
                final Class<?> CycleEditorFragment = XposedHelpers.findClass(
                        "com.android.settings.DataUsageSummary.CycleEditorFragment",
                        lpparam.classLoader);
                findAndHookMethod(CycleEditorFragment, "onCreateDialog", "android.os.Bundle", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onCreateDialog START!");

                        return (Dialog) createAdvDialog(param);

                    }

                });
            }

            //SDK24-25
            if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
                final Class<?> BillingCycleSettings = XposedHelpers.findClass(
                        "com.android.settings.datausage.BillingCycleSettings",
                        lpparam.classLoader);
                final Class<?> CycleEditorFragment = XposedHelpers.findClass(
                        "com.android.settings.datausage.BillingCycleSettings.CycleEditorFragment",
                        lpparam.classLoader);
                findAndHookMethod(CycleEditorFragment, "onCreateDialog", "android.os.Bundle", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onCreateDialog START! " + lpparam.packageName);

                        return createAdvDialog(param);
                    }
                });
                findAndHookMethod(CycleEditorFragment, "onClick", DialogInterface.class, int.class , new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK onClick START!" + lpparam.packageName);

                        final Object args = (Object) XposedHelpers.callMethod(param.thisObject, "getArguments");
                        final Object template = XposedHelpers.callMethod(args, "getParcelable", EXTRA_TEMPLATE );       //type NetworkTemplate
                        final Object target = XposedHelpers.callMethod(param.thisObject, "getTargetFragment");          //type DataUsageEditController
                        final Object editor = XposedHelpers.callMethod(target, "getNetworkPolicyEditor");  //type NetworkPolicyEditor

                        final NumberPicker cycleDayPicker = (NumberPicker) XposedHelpers.getObjectField(param.thisObject, "mCycleDayPicker");
                        final NumberPicker cycleDaysPicker = (NumberPicker) XposedHelpers.getAdditionalStaticField(param.thisObject, "mCycleDaysPicker");
                        final DatePicker cycleDatePicker = (DatePicker) XposedHelpers.getAdditionalStaticField(param.thisObject, "mCycleDatePicker");

                        // clear focus to finish pending text edits
                        cycleDayPicker.clearFocus();
                        cycleDaysPicker.clearFocus();
                        cycleDatePicker.clearFocus();

                        // Encode Day of Month, Month and Duration into one int
                        // via BitShift method.
                        int bs = encodeBitShiftedInt(cycleDatePicker, cycleDaysPicker);

                        //Save in policy CycleDay
                        final String cycleTimezone = new Time().timezone;
                        XposedHelpers.callMethod(editor, "setPolicyCycleDay", template, bs, cycleTimezone);
                        XposedHelpers.callMethod(target, "updateDataUsage");

                        return null;
                    }
                });

                /*
                findAndHookMethod(CycleEditorFragment, "show", BillingCycleSettings, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG) XposedBridge.log("HOOK show START!" + lpparam.packageName);

                        return null;
                    }
                });
                */

            }

        }

        /*
            System UI classes - getDataUsageInfo
         */

        if(         lpparam.packageName.equals("com.android.systemui")
                ||  lpparam.packageName.equals("com.android.settings")
                ){

            if(DEBUG) XposedBridge.log("HOOK SystemUI methods! (pkg:"+lpparam.packageName+")!");

            //SDK22
            //  TODO Adjust for Lollipop SDK22
            //    CLASS_MOBILE_DATA_CONTROLLER_23 = "com.android.systemui.statusbar.policy.MobileDataControllerImpl";
            //    CLASS_MOBILE_DATA_CONTROLLER_22 = "com.android.systemui.statusbar.policy.MobileDataController";
            //    Build.VERSION.SDK_INT >= 22 ? CLASS_MOBILE_DATA_CONTROLLER_23 : CLASS_MOBILE_DATA_CONTROLLER_22;

            //SDK23
            if (Build.VERSION.SDK_INT == 23) {
                final Class<?> MobileDataController = XposedHelpers.findClass(
                        "com.android.systemui.statusbar.policy.MobileDataControllerImpl",
                        lpparam.classLoader);
                final Class<?> NetworkTemplate = XposedHelpers.findClass(
                        "android.net.NetworkTemplate",
                        lpparam.classLoader);
                final Class<?> NetworkStatsHistory = XposedHelpers.findClass(
                        "android.net.NetworkStatsHistory",
                        lpparam.classLoader);
                final Class<?> DataUsageInfo = XposedHelpers.findClass(
                        "com.android.systemui.statusbar.policy.NetworkController$MobileDataController$DataUsageInfo",
                        lpparam.classLoader);
                findAndHookMethod(MobileDataController, "getDataUsageInfo", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG)
                            XposedBridge.log("HOOK android.net > getDataUsageInfo (pkg:" + lpparam.packageName + ")");

                        return getAdvDataUsageInfo(param, NetworkTemplate, NetworkStatsHistory, DataUsageInfo);
                    }
                });
            }


            //SDK24-25
            if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
                final Class<?> DataUsageController = XposedHelpers.findClass(
                        "com.android.settingslib.net.DataUsageController",
                        lpparam.classLoader);
                final Class<?> NetworkTemplate = XposedHelpers.findClass(
                        "android.net.NetworkTemplate",
                        lpparam.classLoader);
                final Class<?> NetworkStatsHistory = XposedHelpers.findClass(
                        "android.net.NetworkStatsHistory",
                        lpparam.classLoader);
                final Class<?> DataUsageInfo = XposedHelpers.findClass(
                        "com.android.settingslib.net.DataUsageController.DataUsageInfo",
                        lpparam.classLoader);
                findAndHookMethod(DataUsageController, "getDataUsageInfo", NetworkTemplate, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG)
                            XposedBridge.log("HOOK android.net > getDataUsageInfo (pkg:" + lpparam.packageName + ")");

                        return getAdvDataUsageInfo(param, NetworkTemplate, NetworkStatsHistory, DataUsageInfo);
                    }
                });

            }

        }



         /*
            Settings App - Billing Cycle preview
            only in SDK 24-25

            status: todo
         */
         /*
        if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            if(         lpparam.packageName.equals("com.android.settings")
                    ) {

                //BillingCyclePreference.java
                final Class<?> BillingCyclePreference = XposedHelpers.findClass(
                        "com.android.settings.datausage.BillingCyclePreference",
                        lpparam.classLoader);
                final Class<?> NetworkTemplate = XposedHelpers.findClass(
                        "android.net.NetworkTemplate",
                        lpparam.classLoader);

                findAndHookMethod(BillingCyclePreference, "setTemplate", new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                        if (DEBUG)
                            XposedBridge.log("HOOK android.net > getDataUsageInfo (pkg:" + lpparam.packageName + ")");

                        return getAdvDataUsageInfo(param, NetworkTemplate, NetworkStatsHistory, DataUsageInfo);
                    }
                });

                //BillingCycleSettings.java

            }
        }
        */
    }

    /****************************
     Main Methods

     */

    //Compute methods
    private static long mComputeLastCycleBoundary(long currentTime, int cycle_day) {
        // Get Preferences
        Calendar pref_cycleDate = Calendar.getInstance();
        //Set no hour
        pref_cycleDate.set(Calendar.HOUR_OF_DAY, 0);
        pref_cycleDate.set(Calendar.MINUTE, 0);
        pref_cycleDate.set(Calendar.SECOND, 0);
        pref_cycleDate.set(Calendar.MILLISECOND, 0);

        int pref_cycleDays = 31;
        // test cycle_day of NetworkPolicy
        if(cycle_day <= 31){
            // Not Bitshifted
            pref_cycleDate.set(Calendar.DAY_OF_MONTH, cycle_day);
        } else {
            // Decode Day of Month, Month and Duration from one int
            // via BitShift method.
            try{
                int bs1 = cycle_day & 0xFF;          // Day of Month
                int bs2 = (cycle_day >> 8) & 0xFF;   // Month
                int bs3 = (cycle_day >> 16) & 0xFF;  // num Days
                if(DEBUG) XposedBridge.log("HOOK REQ loaded bitshited Ints "+bs1+"."+bs2+"."+bs3+"");
                pref_cycleDate.set(Calendar.DAY_OF_MONTH, bs1);
                pref_cycleDate.set(Calendar.MONTH, bs2);
                if(pref_cycleDate.getTimeInMillis() > System.currentTimeMillis()) {
                    pref_cycleDate.set(Calendar.YEAR, pref_cycleDate.get(Calendar.YEAR) - 1);
                    if(DEBUG) XposedBridge.log("HOOK preference year set to "+pref_cycleDate.get(Calendar.YEAR)+"");
                }
                if(bs3 != 0) pref_cycleDays = bs3; else { pref_cycleDays = 31; XposedBridge.log("HOOK ERR pref_cycleDays=0 - forced 31!"); }
            } catch (ClassCastException e){
                if(DEBUG) XposedBridge.log("HOOK REQ Error decoding bitshifted Ints :"+ e.toString() );
            }
        }

        //Debug - Wait... What is the request?
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ LAST Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                                    "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime))); }

        // Approach to date currentTime, when i am close choose Last <- or Next ->
        Calendar cycleDate = (Calendar) pref_cycleDate.clone();
        int m;
        if (cycleDate.getTimeInMillis()>=currentTime) m = -1; else m = 1;
        while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
            cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
        }
        // Set Last Cycle
        if(cycleDate.getTimeInMillis()>=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, -pref_cycleDays);

        //Debug - Ok... That's my result.
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    " LAST to currentTime:"+format.format(new Date(currentTime))+
                    " is "+format.format(new Date(cycleDate.getTimeInMillis())) ); }

        //Return Last
        return cycleDate.getTimeInMillis();
    }

    private static long mComputeNextCycleBoundary(long currentTime, int cycle_day) {
        // Get Preferences
        Calendar pref_cycleDate = Calendar.getInstance();
        //Set no hour
        pref_cycleDate.set(Calendar.HOUR_OF_DAY, 0);
        pref_cycleDate.set(Calendar.MINUTE, 0);
        pref_cycleDate.set(Calendar.SECOND, 0);
        pref_cycleDate.set(Calendar.MILLISECOND, 0);

        int pref_cycleDays = 31;
        // test cycle_day of NetworkPolicy
        if(cycle_day <= 31){
            // Not Bitshifted
            pref_cycleDate.set(Calendar.DAY_OF_MONTH, cycle_day);
        } else {
            // Decode Day of Month, Month and Duration from one int
            // via BitShift method.
            try{
                int bs1 = cycle_day & 0xFF;          // Day of Month
                int bs2 = (cycle_day >> 8) & 0xFF;   // Month
                int bs3 = (cycle_day >> 16) & 0xFF;  // num Days
                if(DEBUG) XposedBridge.log("HOOK REQ loaded bitshited Ints "+bs1+"."+bs2+"."+bs3+"");
                pref_cycleDate.set(Calendar.DAY_OF_MONTH, bs1);
                pref_cycleDate.set(Calendar.MONTH, bs2);
                if(pref_cycleDate.getTimeInMillis() > System.currentTimeMillis()) {
                    pref_cycleDate.set(Calendar.YEAR, pref_cycleDate.get(Calendar.YEAR) - 1);
                    if(DEBUG) XposedBridge.log("HOOK REQ preference year set to "+pref_cycleDate.get(Calendar.YEAR)+"");
                }
                if(bs3 != 0) pref_cycleDays = bs3; else { pref_cycleDays = 31; XposedBridge.log("HOOK ERR pref_cycleDays=0 - forced 31!"); }
            } catch (ClassCastException e){
                if(DEBUG) XposedBridge.log("HOOK REQ Error decoding bitshifted Ints :"+ e.toString() );
            }
        }

        //Debug - Wait... What is the request?
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK REQ NEXT Cycle with prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    "*"+pref_cycleDays+"  for currentTime:"+format.format(new Date(currentTime))); }

        // Approach to date currentTime, when i am close choose Last <- or Next ->
        Calendar cycleDate = (Calendar) pref_cycleDate.clone();
        int m;
        if (cycleDate.getTimeInMillis()>currentTime) m = -1; else m = 1;
        while ( daysBetween(cycleDate.getTimeInMillis(), currentTime) >= pref_cycleDays ) {
            if(DEBUG) XposedBridge.log("HOOK               " + m + " " + pref_cycleDays );
            cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays * m);
        }
        // Set Next Cycle
        if(cycleDate.getTimeInMillis()<=currentTime) cycleDate.add(Calendar.DAY_OF_YEAR, pref_cycleDays);

        //Debug - Ok... That's my result.
        if(DEBUG) { Format format = new SimpleDateFormat("dd/MM/yyyy");
                    XposedBridge.log("HOOK       from prefTime:"+format.format(new Date(pref_cycleDate.getTimeInMillis()))+
                    " NEXT to currentTime:"+format.format(new Date(currentTime))+
                    " is "+format.format(new Date(cycleDate.getTimeInMillis())) ); }

        //Return Last
        return cycleDate.getTimeInMillis();
    }

    //Dialog methods
    private static Object createAdvDialog(XC_MethodHook.MethodHookParam param) {

        XposedBridge.log("HOOK createAdvDialog start!");

        DialogFragment mCycleEditorFragment = (DialogFragment) param.thisObject;                    //CycleEditorFragment
        final Context context = (Context) XposedHelpers.callMethod(param.thisObject, "getActivity");

        final Object target = XposedHelpers.callMethod(param.thisObject, "getTargetFragment");
        final Object editor;                                                                        //type NetworkPolicyEditor
        if (Build.VERSION.SDK_INT == 23) {
            editor = XposedHelpers.getObjectField(target, "mPolicyEditor");
        } else if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            editor = XposedHelpers.callMethod(target, "getNetworkPolicyEditor");
        } else {
            XposedBridge.log("HOOK createAdvDialog: SDK "+Build.VERSION.SDK_INT+" not supported!");
            return null;
        }

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final LayoutInflater dialogInflater = LayoutInflater.from(builder.getContext());

        final View view = (View) XposedHelpers.callMethod(dialogInflater, "inflate", R_layout_data_usage_cycle_editor, null, false);
        final NumberPicker cycleDayPicker;
        if (Build.VERSION.SDK_INT == 23) {
            cycleDayPicker = (NumberPicker) view.findViewById(R_id_cycle_day);
        } else if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            XposedHelpers.setObjectField(param.thisObject, "mCycleDayPicker", (NumberPicker) view.findViewById(R_id_cycle_day));
            cycleDayPicker = (NumberPicker) XposedHelpers.getObjectField(param.thisObject, "mCycleDayPicker");
        } else {
            Toast.makeText(context, "SDK "+Build.VERSION.SDK_INT+" not supported!", Toast.LENGTH_LONG).show();
            return null;
        }

        final Object args = (Object) XposedHelpers.callMethod(param.thisObject, "getArguments");
        final Object template = XposedHelpers.callMethod(args, "getParcelable", EXTRA_TEMPLATE );       //type NetworkTemplate
        final int cycleDay = (int) XposedHelpers.callMethod(editor, "getPolicyCycleDay", template);

        if(DEBUG) XposedBridge.log("HOOK R_id_cycle_day="+R_id_cycle_day);
        if(DEBUG) XposedBridge.log("HOOK R_id_cycle_days="+R_id_cycle_days);
        if(DEBUG) XposedBridge.log("HOOK R_id_datepicker="+R_id_datepicker);
        //view_dump(view);

        //Decode cycleDay
        Calendar pref_cycle_date = Calendar.getInstance();
        int pref_cycle_days = 31;
        if (DEBUG) XposedBridge.log("HOOK pref LOAD " + cycleDay + "");
        if (cycleDay <= 31) {
            pref_cycle_date.set(Calendar.DAY_OF_MONTH, cycleDay);
        } else {
            try {
                int bs1 = cycleDay & 0xFF;          // Day of Month
                int bs2 = (cycleDay >> 8) & 0xFF;   // Month
                int bs3 = (cycleDay >> 16) & 0xFF;  // num Days
                if (DEBUG) XposedBridge.log("HOOK pref LOAD BITSHIFT " + bs1 + "." + bs2 + "." + bs3 + "");
                pref_cycle_date.set(Calendar.DAY_OF_MONTH, bs1);
                pref_cycle_date.set(Calendar.MONTH, bs2);
                if (pref_cycle_date.getTimeInMillis() > System.currentTimeMillis()) {
                    pref_cycle_date.set(Calendar.YEAR, pref_cycle_date.get(Calendar.YEAR) - 1);
                    if (DEBUG) XposedBridge.log("HOOK pref year set to " + pref_cycle_date.get(Calendar.YEAR) + "");
                }
                if (bs3 != 0) pref_cycle_days = bs3;
                else {
                    pref_cycle_days = 31;
                    XposedBridge.log("HOOK ERR pref_cycle_days=0 - forced 31!");
                }
            } catch (ClassCastException e) {
                //Not a viewGroup here
                if (DEBUG) XposedBridge.log("HOOK pref decoding error " + e.toString());
                e.printStackTrace();
            }
        }

        //Update pref_cycle_date to Last Cycle
        while (pref_cycle_date.getTimeInMillis() < System.currentTimeMillis()) {     // Cycle until cycle_date > currentTime
            pref_cycle_date.add(Calendar.DAY_OF_MONTH, pref_cycle_days);
            if (DEBUG)
                XposedBridge.log("HOOK pref pref_cycle_date update to " + pref_cycle_date + "");
        }
        pref_cycle_date.add(Calendar.DAY_OF_MONTH, -pref_cycle_days);                       //Set Last Cycle

        //Set layout cycleDayPicker
        if (DEBUG) XposedBridge.log("HOOK Numberpicker = " + cycleDayPicker.getId() + " " + cycleDayPicker.toString());
        cycleDayPicker.setMinValue(1);
        cycleDayPicker.setMaxValue(31);
        cycleDayPicker.setValue(cycleDay);
        cycleDayPicker.setWrapSelectorWheel(true);

        //Set layout cycleDaysPicker
        final NumberPicker cycleDaysPicker = (NumberPicker) view.findViewById(R_id_cycle_days);
        String[] values = new String[100];
        for (int i = 0; i < 100; ++i) {
            values[i] = "" + (i + 1);
        }
        values[0] = context.getString(modR_strings_nr1_daily);
        values[6] = context.getString(modR_strings_nr7_weekly);
        values[29] = context.getString(modR_strings_nr30_fixedmonth);
        values[30] = context.getString(modR_strings_nr31_monthly);
        cycleDaysPicker.setDisplayedValues(values);
        cycleDaysPicker.setMinValue(1);
        cycleDaysPicker.setMaxValue(100);
        cycleDaysPicker.setValue(pref_cycle_days);
        cycleDaysPicker.setWrapSelectorWheel(true);

        //Set layout cycleDatePicker
        final DatePicker cycleDatePicker = (DatePicker) view.findViewById(R_id_datepicker);
        int year = pref_cycle_date.get(Calendar.YEAR);
        int month = pref_cycle_date.get(Calendar.MONTH);
        int day = pref_cycle_date.get(Calendar.DAY_OF_MONTH);
        cycleDatePicker.updateDate(year, month, day);
        cycleDatePicker.setMaxDate(System.currentTimeMillis());

        // Set builder
        builder.setTitle("Advanced Cycle Editor");  //R.string.data_usage_cycle_editor_title
        builder.setView(view);
        if (Build.VERSION.SDK_INT == 23) {
            builder.setPositiveButton("OK",             //R.string.data_usage_cycle_editor_positive
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // clear focus to finish pending text edits
                            cycleDayPicker.clearFocus();
                            cycleDaysPicker.clearFocus();
                            cycleDatePicker.clearFocus();
                            int bs = encodeBitShiftedInt(cycleDatePicker, cycleDaysPicker);


                            //Save in policy CycleDay
                            final String cycleTimezone = new Time().timezone;
                            XposedHelpers.callMethod(editor, "setPolicyCycleDay", template, bs, cycleTimezone);
                            XposedHelpers.callMethod(target, "updatePolicy", true);

                        }
                    });
        } else if (Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            XposedHelpers.setAdditionalStaticField(param.thisObject,"mCycleDatePicker",cycleDatePicker);
            XposedHelpers.setAdditionalStaticField(param.thisObject,"mCycleDaysPicker",cycleDaysPicker);
            builder.setPositiveButton("OK", (DialogInterface.OnClickListener) param.thisObject);
        }

        //Create Dialog & return
        if (DEBUG) XposedBridge.log("HOOK onCreateDialog END!");
        return builder.create();
    }

    private static int encodeBitShiftedInt(DatePicker cycleDatePicker, NumberPicker cycleDaysPicker) {
        // Encode Day of Month, Month and Duration into one int
        // via BitShift method.
        int bs1 = cycleDatePicker.getDayOfMonth();
        int bs2 = cycleDatePicker.getMonth();
        int bs3 = cycleDaysPicker.getValue();
        int bs = (bs1 & 0xFF) | ((bs2 & 0xFF) << 8) | ((bs3 & 0xFF) << 16);
        if (DEBUG) XposedBridge.log("HOOK pref SAVED " + bs + " (" + bs1 + "." + bs2 + "." + bs3 + ")");
        return bs;
    }

    //Status Bar methods
    private static Object getAdvDataUsageInfo(XC_MethodHook.MethodHookParam param, Class<?> networkTemplate, Class<?> networkStatsHistory, Class<?> dataUsageInfo) {

        //Get Session
        Object session = XposedHelpers.callMethod(param.thisObject, "getSession");    //INetworkStatsSession
        if (session == null) {
            return XposedHelpers.callMethod(param.thisObject, "warn", "no stats session");
        }

        //Get Template
        Object template = null;
        if(Build.VERSION.SDK_INT == 23) {
            final Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");
            final String subscriberId = (String) XposedHelpers.callMethod(param.thisObject, "getActiveSubscriberId", context);   //String
            if (subscriberId == null) {
                XposedHelpers.callMethod(param.thisObject, "warn", "no subscriber id");
            }
            if (DEBUG) XposedBridge.log("HOOK UI DataUsage: subscriberId :" + subscriberId + "");
            Object mTelephonyManager = XposedHelpers.getObjectField(param.thisObject, "mTelephonyManager");
            template = XposedHelpers.callStaticMethod(networkTemplate, "buildTemplateMobileAll", subscriberId);
            template = XposedHelpers.callStaticMethod(networkTemplate, "normalize", template, XposedHelpers.callMethod(mTelephonyManager, "getMergedSubscriberIds"));
        }
        else if(Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) {
            template = param.args[0];
        }

        final Object policy = XposedHelpers.callMethod(param.thisObject, "findNetworkPolicy", template);
        try {
            if(Build.VERSION.SDK_INT == 23) session = XposedHelpers.getObjectField(param.thisObject, "mSession");

            //Initialize vars
            int FIELD_RX_BYTES = XposedHelpers.getStaticIntField(networkStatsHistory, "FIELD_RX_BYTES");
            int FIELD_TX_BYTES = XposedHelpers.getStaticIntField(networkStatsHistory, "FIELD_TX_BYTES");
            int FIELDS = FIELD_RX_BYTES | FIELD_TX_BYTES;
            final Object history = XposedHelpers.callMethod(session, "getHistoryForNetwork", template, FIELDS); //type NetworkStatsHistory
            final long now = System.currentTimeMillis();
            final long start, end;

            //Compute Cycle boundaries
            int cycleDay = 0;
            if (policy != null) { cycleDay = (int) XposedHelpers.getObjectField(policy, "cycleDay"); }
            if (policy != null && cycleDay > 31) {
                start = mComputeLastCycleBoundary(now, cycleDay);
                end = mComputeNextCycleBoundary(now, cycleDay);
            } else {
                // period = last 4 wks
                end = now;
                start = now - DateUtils.WEEK_IN_MILLIS * 4;
            }

            //Formatting and finalize values
            final long callStart = System.currentTimeMillis();
            final Object entry = XposedHelpers.callMethod(history, "getValues", start, end, now, null);     //type NetworkStatsHistory.Entry
            final long callEnd = System.currentTimeMillis();
            if (DEBUG) XposedBridge.log("HOOK UI DataUsage: History call from " +
                    new Date(start) + " to " + new Date(end) + " now=" + new Date(now) + " took " + (callEnd - callStart) + ": " +
                    (String) XposedHelpers.callMethod(param.thisObject, "historyEntryToString", entry));
            if (entry == null) {
                //return warn("no entry data");
                if (DEBUG) XposedBridge.log("HOOK UI DataUsage: entry is null");
            }
            final long totalBytes = (long) XposedHelpers.getLongField(entry, "rxBytes") + (long) XposedHelpers.getLongField(entry, "txBytes");

            //Create dataUsageInfo and return
            final Object usage = XposedHelpers.newInstance(dataUsageInfo);
            if(Build.VERSION.SDK_INT == 24 || Build.VERSION.SDK_INT == 25) { XposedHelpers.setLongField(usage, "startDate", (long) start); }
            XposedHelpers.setLongField(usage, "usageLevel", (long) totalBytes);
            XposedHelpers.setObjectField(usage, "period", (String) XposedHelpers.callMethod(param.thisObject, "formatDateRange", start, end));
            if (policy != null) {
                XposedHelpers.setLongField(usage, "limitLevel", (long) XposedHelpers.getLongField(policy, "limitBytes") > 0 ? (long) XposedHelpers.getLongField(policy, "limitBytes") : 0);
                XposedHelpers.setLongField(usage, "warningLevel", (long) XposedHelpers.getLongField(policy, "warningBytes") > 0 ? (long) XposedHelpers.getLongField(policy, "warningBytes") : 0);
            } else {
                XposedHelpers.setLongField(usage, "warningLevel", 2L * 1024 * 1024 * 1024);
            }
            Object mNetworkController = XposedHelpers.getObjectField(param.thisObject, "mNetworkController");
            if (usage != null && mNetworkController != null) {
                XposedHelpers.setObjectField(usage, "carrier", (String) XposedHelpers.callMethod(mNetworkController, "getMobileDataNetworkName"));
            }
            if (DEBUG && usage != null)XposedBridge.log("HOOK UI DataUsage: usageLevel=" + XposedHelpers.getObjectField(usage, "usageLevel") + " period=" + XposedHelpers.getObjectField(usage, "period") + "");

            return usage;
        } catch (Exception e) {
            if (DEBUG) XposedBridge.log("HOOK UI DataUsage: remote call failed StackTrace:" + Log.getStackTraceString(e));
            return XposedHelpers.callMethod(param.thisObject, "warn", "remote call failed");
        }

        //Return Null
        //if (DEBUG) XposedBridge.log("HOOK UI DataUsage: returning null!");
        //return null;
    }


    /****************************
     Utility Methods
     */

    public static void view_dump(View view){
        if(DEBUG) XposedBridge.log( "HOOK DUMP view " + view.toString());
        try {
            ViewGroup rootView = (ViewGroup) view.getRootView();
            int childViewCount = rootView.getChildCount();
            for (int i=0; i<childViewCount;i++){
                View workWithMe = rootView.getChildAt(i);
                if(DEBUG) XposedBridge.log( "HOOK DUMP view found {" + workWithMe.getId()+"} : "+ workWithMe.toString() + " " + workWithMe.getClass().getName().toString() + " " );
                //if(workWithMe instanceof LinearLayout ){
                    if(DEBUG) XposedBridge.log( "HOOK DUMP             " + workWithMe.getClass().getName().toString() + " contains:" );
                    ViewGroup llworkWithme = (ViewGroup) workWithMe;
                    int llchildViewCount = llworkWithme.getChildCount();
                    for (int lli=0; lli<llchildViewCount;lli++){
                        View llview = llworkWithme.getChildAt(lli);
                        if(DEBUG) XposedBridge.log( "HOOK DUMP              + {" + llview.getId()+"} : "+ llview.toString() + " " );
                    }
                //}
            }
        } catch (ClassCastException e){
            //Not a viewGroup here
            if(DEBUG) XposedBridge.log("HOOK DUMP view exception  Not a viewGroup here "+ e.toString() );
        } catch (NullPointerException e){
            //Root view is null
            if(DEBUG) XposedBridge.log("HOOK DUMP view exception  Root view is null "+ e.toString() );
        }
    }

    public static int daysBetween(long day1, long day2){
        Calendar dayOne = Calendar.getInstance();
        dayOne.setTimeInMillis(day1);
        Calendar dayTwo = Calendar.getInstance();
        dayTwo.setTimeInMillis(day2);

        if (dayOne.get(Calendar.YEAR) == dayTwo.get(Calendar.YEAR)) {
            return Math.abs(dayOne.get(Calendar.DAY_OF_YEAR) - dayTwo.get(Calendar.DAY_OF_YEAR));
        } else {
            if (dayTwo.get(Calendar.YEAR) > dayOne.get(Calendar.YEAR)) {
                //swap them
                Calendar temp = dayOne;
                dayOne = dayTwo;
                dayTwo = temp;
            }
            int extraDays = 0;

            int dayOneOriginalYearDays = dayOne.get(Calendar.DAY_OF_YEAR);

            while (dayOne.get(Calendar.YEAR) > dayTwo.get(Calendar.YEAR)) {
                dayOne.add(Calendar.YEAR, -1);
                // getActualMaximum() important for leap years
                extraDays += dayOne.getActualMaximum(Calendar.DAY_OF_YEAR);
            }

            return extraDays - dayTwo.get(Calendar.DAY_OF_YEAR) + dayOneOriginalYearDays ;
        }
    }

}


