package de.blau.android.easyedit;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import de.blau.android.App;
import de.blau.android.Logic;
import de.blau.android.Main;
import de.blau.android.Map;
import de.blau.android.R;
import de.blau.android.SignalHandler;
import de.blau.android.TestUtils;
import de.blau.android.osm.ApiTest;
import de.blau.android.osm.Node;
import de.blau.android.osm.Way;
import de.blau.android.prefs.AdvancedPrefDatabase;
import de.blau.android.prefs.Preferences;
import de.blau.android.resources.TileLayerServer;
import de.blau.android.tasks.Note;
import de.blau.android.tasks.Task;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SimpleActionsTest {

    Context              context = null;
    AdvancedPrefDatabase prefDB  = null;
    Main                 main    = null;
    UiDevice             device  = null;
    Map                  map     = null;
    Logic                logic   = null;

    @Rule
    public ActivityTestRule<Main> mActivityRule = new ActivityTestRule<>(Main.class);

    /**
     * Pre-test setup
     */
    @Before
    public void setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        main = mActivityRule.getActivity();
        Preferences prefs = new Preferences(context);
        prefs.setBackGroundLayer(TileLayerServer.LAYER_NONE); // try to avoid downloading tiles
        prefs.setOverlayLayer(TileLayerServer.LAYER_NOOVERLAY);
        prefs.enableSimpleActions(true);
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                main.showSimpleActionsButton();   
            }            
        });

        map = main.getMap();
        map.setPrefs(main, prefs);
        TestUtils.grantPermissons();
        TestUtils.dismissStartUpDialogs(main);
        final CountDownLatch signal1 = new CountDownLatch(1);
        logic = App.getLogic();
        logic.deselectAll();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream is = loader.getResourceAsStream("test2.osm");
        logic.readOsmFile(main, is, false, new SignalHandler(signal1));
        try {
            signal1.await(ApiTest.TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        try {
            is.close();
        } catch (IOException e1) {
        }
        App.getTaskStorage().reset();
        TestUtils.stopEasyEdit(main);
    }

    /**
     * Post-test teardown
     */
    @After
    public void teardown() {
        if (logic != null) {
            logic.deselectAll();
        }
        TestUtils.zoomToLevel(main, 18);
        App.getTaskStorage().reset();
    }

    /**
     * Create a new Node
     */
    @Test
    public void newNode() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(main, 21);
        TestUtils.unlock();
        TestUtils.clickButton("de.blau.android:id/simpleButton", true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_node), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_node)));
        TestUtils.clickAtCoordinates(map, 8.3893454, 47.3901898, true);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_nodeselect)));

        Node node = App.getLogic().getSelectedNode();
        Assert.assertNotNull(node);
        Assert.assertTrue(node.getOsmId() < 0);

        TestUtils.clickHome(device);
    }

    /**
     * Create a new way from menu and clicks at two more locations and finishing via home button
     */
    @Test
    public void newWay() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(main, 21);
        TestUtils.unlock();
        TestUtils.clickButton("de.blau.android:id/simpleButton", true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_way), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_way)));
        TestUtils.clickAtCoordinates(map, 8.3893454, 47.3901898, true);
        device.waitForIdle(1000);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_createpath)));
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(map, 8.3895763, 47.3901374, false);
        device.waitForIdle(1000);
        TestUtils.clickAtCoordinates(map, 8.3896274, 47.3902424, false);
        device.waitForIdle(1000);
        TestUtils.clickHome(device);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.tag_form_unknown_element)));
        TestUtils.clickUp(device);
        Way way = App.getLogic().getSelectedWay();
        Assert.assertNotNull(way);
        Assert.assertTrue(way.getOsmId() < 0);
        Assert.assertEquals(3, way.nodeCount());
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.actionmode_wayselect)));
        TestUtils.clickHome(device);
    }

    /**
     * Create a new Note
     */
    @Test
    public void newBug() {
        map.getDataLayer().setVisible(true);
        TestUtils.zoomToLevel(main, 21);
        TestUtils.unlock();
        TestUtils.clickButton("de.blau.android:id/simpleButton", true);
        Assert.assertTrue(TestUtils.clickText(device, false, context.getString(R.string.menu_add_map_note), true));
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.simple_add_note)));
        TestUtils.clickAtCoordinates(map, 8.3890736, 47.3896628, true);
        Assert.assertTrue(TestUtils.findText(device, false, context.getString(R.string.openstreetbug_new_title)));
        UiObject editText = device.findObject(new UiSelector().clickable(true).resourceId("de.blau.android:id/openstreetbug_comment"));
        try {
            editText.click(); // NOTE this seems to be necessary
            editText.setText("test");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertTrue(TestUtils.clickText(device, true, context.getString(R.string.Save), true));
        List<Task> tasks = App.getTaskStorage().getTasks();
        Assert.assertTrue(tasks.size() == 1);
        Task t = tasks.get(0);
        Assert.assertTrue(t instanceof Note);
        Assert.assertTrue("test".equals(((Note) t).getComment()));
    }
}
