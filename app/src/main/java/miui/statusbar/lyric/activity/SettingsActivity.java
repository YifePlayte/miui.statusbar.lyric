package miui.statusbar.lyric.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.byyang.choose.ChooseFileUtils;
import miui.statusbar.lyric.Config;
import miui.statusbar.lyric.R;
import miui.statusbar.lyric.Utils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.Objects;



@SuppressLint("ExportedPreferenceActivity")
public class SettingsActivity extends PreferenceActivity {
    private Config config;
    private final Activity activity = this;

    @SuppressLint("WrongConstant")
    @SuppressWarnings({"ResultOfMethodCallIgnored", "deprecation"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.root_preferences);
        Utils.checkPermissions(activity);

        config = new Config();


        SharedPreferences preferences = activity.getSharedPreferences("protocol", 0);
        boolean count = preferences.getBoolean("protocol", false);
        if (!count) {
            new AlertDialog.Builder(activity)
                    .setTitle("警告")
                    .setMessage("本软件发布不久，可能会有许多BUG\n" +
                            "使用本模块造成的破坏，软件不负责\n" +
                            "继续代表同意")
                    .setNegativeButton("继续", (dialog, which) -> {
                        SharedPreferences.Editor a = preferences.edit();
                        a.putBoolean("protocol", true);
                        a.apply();
                    })
                    .setPositiveButton("不同意", (dialog, which) -> activity.finish())
                    .setCancelable(false)
                    .create()
                    .show();
        }


        // 隐藏桌面图标
        SwitchPreference hideIcons = (SwitchPreference) findPreference("hideLauncherIcon");
        assert hideIcons != null;
        hideIcons.setChecked(config.getHideLauncherIcon());
        hideIcons.setOnPreferenceChangeListener((preference, newValue) -> {
            int mode;
            PackageManager packageManager = Objects.requireNonNull(activity).getPackageManager();
            if ((Boolean) newValue) {
                mode = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
            } else {
                mode = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
            }
            packageManager.setComponentEnabledSetting(new ComponentName(activity, "miui.statusbar.lyric.launcher"), mode, PackageManager.DONT_KILL_APP);
            config.setHideLauncherIcon((Boolean) newValue);
            return true;
        });

        // 歌词总开关
        SwitchPreference lyricService = (SwitchPreference) findPreference("lyricService");
        assert lyricService != null;
        lyricService.setChecked(config.getLyricService());
        lyricService.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setLyricService((Boolean) newValue);
            return true;
        });

        // 暂停关闭歌词
        SwitchPreference lyricOff = (SwitchPreference) findPreference("lyricOff");
        assert lyricOff != null;
        lyricOff.setChecked(config.getLyricAutoOff());
        lyricOff.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setLyricAutoOff((Boolean) newValue);
            return true;
        });

        // 歌词最大自适应宽度
        EditTextPreference lyricMaxWidth = (EditTextPreference) findPreference("lyricMaxWidth");
        assert lyricMaxWidth != null;
        lyricMaxWidth.setEnabled(String.valueOf(config.getLyricWidth()).equals("-1"));
        lyricMaxWidth.setSummary((String.valueOf(config.getLyricMaxWidth())));
        if (String.valueOf(config.getLyricMaxWidth()).equals("-1")) {
            lyricMaxWidth.setSummary("关闭");
        }
        lyricMaxWidth.setDialogMessage("(-1~100，-1为关闭，仅在歌词宽度为自适应时生效)，当前:" + lyricMaxWidth.getSummary());
        lyricMaxWidth.setOnPreferenceChangeListener((preference, newValue) -> {
            lyricMaxWidth.setDialogMessage("(-1~100，-1为关闭，仅在歌词宽度为自适应时生效)，当前:自适应");
            config.setLyricMaxWidth(-1);
            lyricMaxWidth.setSummary("自适应");
            config.setLyricMaxWidth(-1);
            try {
                String value = newValue.toString().replaceAll(" ", "").replaceAll("\n","");
                if (value.equals("-1")) {
                } else if (Integer.parseInt(value) <= 100 && Integer.parseInt(value) >= 0) {
                    config.setLyricMaxWidth(Integer.parseInt(value));
                    lyricMaxWidth.setSummary(value);
                } else {
                    Toast.makeText(activity, "范围输入错误，恢复默认", Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(activity, "范围输入错误，恢复默认", Toast.LENGTH_LONG).show();
            }
            return true;
        });

        // 歌词宽度
        EditTextPreference lyricWidth = (EditTextPreference) findPreference("lyricWidth");
        assert lyricWidth != null;
        lyricWidth.setSummary(String.valueOf(config.getLyricWidth()));
        if (String.valueOf(config.getLyricWidth()).equals("-1")) {
            lyricWidth.setSummary("自适应");
        }
        lyricWidth.setDefaultValue(String.valueOf(config.getLyricWidth()));
        lyricWidth.setDialogMessage("(-1~100，-1为自适应)，当前：" + lyricWidth.getSummary());
        lyricWidth.setOnPreferenceChangeListener((preference, newValue) -> {
            String value = newValue.toString().replaceAll(" ", "").replaceAll("\n","");
            lyricMaxWidth.setEnabled(true);
            lyricWidth.setSummary("自适应");
            lyricWidth.setDialogMessage("(-1~100，-1为自适应)，当前：自适应");
            config.setLyricWidth(-1);

            try {
                if (value.equals("-1")) {
                } else if (Integer.parseInt(value) <= 100 && Integer.parseInt(value) >= 0) {
                    config.setLyricWidth(Integer.parseInt(value));
                    lyricWidth.setSummary(value);
                    lyricMaxWidth.setEnabled(false);
                    lyricWidth.setDialogMessage("(-1~100，-1为自适应)，当前：" + value);
                } else {
                    Toast.makeText(activity, "范围输入错误，恢复默认", Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(activity, "范围输入错误，恢复默认", Toast.LENGTH_LONG).show();
            }
            return true;
        });


        // 歌词颜色
        EditTextPreference lyricColour = (EditTextPreference) findPreference("lyricColour");
        assert lyricColour != null;
        lyricColour.setSummary(config.getLyricColor());
        if (config.getLyricColor().equals("off")) {
            lyricColour.setSummary("自适应");
        }
        lyricColour.setDefaultValue(String.valueOf(config.getLyricColor()));
        lyricColour.setDialogMessage("请输入16进制颜色代码，例如: #C0C0C0，目前：" + config.getLyricColor());
        lyricColour.setOnPreferenceChangeListener((preference, newValue) -> {
            String value = newValue.toString().replaceAll(" ", "");
            if (value.equals("") | value.equals("关闭") | value.equals("自适应")) {
                config.setLyricColor("off");
                lyricColour.setSummary("自适应");
            } else {
                try {
                    Color.parseColor(newValue.toString());
                    config.setLyricColor(newValue.toString());
                    lyricColour.setSummary(newValue.toString());
                } catch (Exception e) {
                    config.setLyricColor("off");
                    lyricColour.setSummary("自适应");
                    Toast.makeText(activity, "颜色代码不正确!", Toast.LENGTH_LONG).show();
                }
            }
            return true;
        });


        // 歌词图标
        SwitchPreference icon = (SwitchPreference) findPreference("lyricIcon");
        assert icon != null;
        icon.setChecked(config.getIcon());
        icon.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setIcon((Boolean) newValue);
            return true;
        });

        // 歌词时间切换
        SwitchPreference lyricSwitch = (SwitchPreference) findPreference("lyricSwitch");
        assert lyricSwitch != null;
        lyricSwitch.setChecked(config.getLyricSwitch());
        lyricSwitch.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setLyricSwitch((Boolean) newValue);
            return true;
        });

        // 图标路径
        Preference iconPath = findPreference("iconPath");
        assert iconPath != null;
        iconPath.setSummary(config.getIconPath());
        if (config.getIconPath().equals(Utils.PATH)) {
            iconPath.setSummary("默认路径");
        }
        iconPath.setOnPreferenceClickListener(((preference) -> {
            new AlertDialog.Builder(activity)
                    .setTitle("图标路径")
                    .setNegativeButton("恢复默认路径", (dialog, which) -> {
                        iconPath.setSummary("默认路径");
                        config.setIconPath(Utils.PATH);
                        Utils.initIcon(activity);
                    })
                    .setPositiveButton("选择新路径", (dialog, which) -> {
                        ChooseFileUtils chooseFileUtils = new ChooseFileUtils(activity);
                        chooseFileUtils.chooseFolder(new ChooseFileUtils.ChooseListener() {
                            @Override
                            public void onSuccess(String filePath, Uri uri, Intent intent) {
                                super.onSuccess(filePath, uri, intent);
                                config.setIconPath(filePath);
                                iconPath.setSummary(filePath);
                                if (config.getIconPath().equals(Utils.PATH)) {
                                    iconPath.setSummary("默认路径");
                                }
                                Utils.initIcon(activity);
                            }
                        });
                    })
                    .create()
                    .show();


            return true;
        }));

        // 图标反色
        SwitchPreference iconColor = (SwitchPreference) findPreference("iconAutoColor");
        assert iconColor != null;
        iconColor.setSummary("关闭");
        if (config.getIconAutoColor()) {
            iconColor.setSummary("开启");
        }
        iconColor.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setIconAutoColor((boolean) newValue);
            return true;
        });


        // 隐藏通知图标
        SwitchPreference hideNoticeIcon = (SwitchPreference) findPreference("hideNoticeIcon");
        assert hideNoticeIcon != null;
        hideNoticeIcon.setChecked(config.getHideNoticeIcon());
        hideNoticeIcon.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setHideNoticeIcon((Boolean) newValue);
            return true;
        });

        // 隐藏实时网速
        SwitchPreference hideNetWork = (SwitchPreference) findPreference("hideNetWork");
        assert hideNetWork != null;
        hideNetWork.setChecked(config.getHideNetSpeed());
        hideNetWork.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setHideNetSpeed((Boolean) newValue);
            return true;
        });

        // 隐藏运营商名称
        SwitchPreference hideCUK = (SwitchPreference) findPreference("hideCUK");
        assert hideCUK != null;
        hideCUK.setChecked(config.getHideCUK());
        hideCUK.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setHideCUK((Boolean) newValue);
            return true;
        });

        // Debug模式
        SwitchPreference debug = (SwitchPreference) findPreference("debug");
        assert debug != null;
        debug.setChecked(config.getDebug());
        debug.setOnPreferenceChangeListener((preference, newValue) -> {
            config.setDebug((Boolean) newValue);
            return true;
        });

        // 重启SystemUI
        Preference reSystemUI = findPreference("restartUI");
        assert reSystemUI != null;
        reSystemUI.setOnPreferenceClickListener(((preference) -> {
            new AlertDialog.Builder(activity)
                    .setTitle("确定重启系统界面吗？")
                    .setMessage("若使用中突然发现不能使用，可尝试重启系统界面。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        try {
                            Process p = Runtime.getRuntime().exec("su");
                            OutputStream outputStream = p.getOutputStream();
                            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
                            dataOutputStream.writeBytes("pkill -f com.android.systemui");
                            dataOutputStream.flush();
                            dataOutputStream.close();
                            outputStream.close();
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
            return true;
        }));

        // 重置插件
        Preference reset = findPreference("reset");
        assert reset != null;
        reset.setOnPreferenceClickListener((preference) -> {
            new AlertDialog.Builder(activity)
                    .setTitle("是否要重置模块")
                    .setMessage("模块没问题请不要随意重置")
                    .setPositiveButton("确定", (dialog, which) -> {
                        SharedPreferences userSettings = activity.getSharedPreferences("miui.statusbar.lyric_preferences", 0);
                        SharedPreferences.Editor editor = userSettings.edit();
                        editor.clear();
                        editor.apply();
                        new File(Utils.PATH + "Config.json").delete();
                        Toast.makeText(activity, "重置成功", Toast.LENGTH_LONG).show();
                        activity.finishAffinity();
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
            return true;
        });


        //检查更新
        Preference checkUpdate = findPreference("CheckUpdate");
        assert checkUpdate != null;
        checkUpdate.setSummary("当前版本: " + Utils.getLocalVersion(activity));
        checkUpdate.setOnPreferenceClickListener((preference) -> {
            Utils.checkUpdate(activity);
            return true;
        });

        // 作者主页
        Preference author = findPreference("about");
        assert author != null;
        author.setOnPreferenceClickListener((preference) -> {
            startActivity(new Intent(activity, AboutActivity.class));
            return true;
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == 0) {
            Utils.init(activity);
            Utils.initIcon(activity);
        } else {
            new AlertDialog.Builder(activity)
                    .setIcon(R.drawable.ic_launcher_foreground)
                    .setTitle("获取存储权限失败")
                    .setMessage("请开通存储权限\n否则无法正常使用本应用\n若不信任本软件,请卸载")
                    .setNegativeButton("重新申请", (dialog, which) -> Utils.checkPermissions(activity))
                    .setPositiveButton("卸载本软件", (dialog, which) -> {
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
                        startActivity(intent);
                    })
                    .setNeutralButton("前往设置授予权限", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.fromParts("package", getPackageName(), null));
                        startActivityForResult(intent, 13131);
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 13131) {
            Utils.checkPermissions(activity);
        }
    }

}