package org.catrobat.catroid.livewallpaper.test;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.livewallpaper.ui.LiveWallpaperSettings;
import com.jayway.android.robotium.solo.Solo;
import org.catrobat.catroid.livewallpaper.R;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;


public class SettingsUITest extends
		ActivityInstrumentationTestCase2<LiveWallpaperSettings> {

	private Solo solo;
	private static final int DELAY = 1500;
	public SettingsUITest() {
		super(LiveWallpaperSettings.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		solo = new Solo(getInstrumentation(),getActivity());
		if(ProjectManager.getInstance().getCurrentProject()==null)
		{
			Log.d("LWP", "Current Project not set!!");
			ProjectManager.getInstance().loadProject(solo.getString(R.string.default_project_name),getActivity().getApplicationContext(),false);
		    
		}
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testComingUp()
	{	
		solo.assertCurrentActivity("Checking on Activity",LiveWallpaperSettings.class);
	}
	
	public void testAboutButtton()
	{
		solo.clickOnText(solo.getString(R.string.about_this_wallpaper));
		solo.sleep(DELAY);
		assertTrue("The about this wallpaper title was not found", solo.searchText(solo.getString(R.string.about_this_wallpaper)));
		Log.d("LWP","About Title is: " + solo.getString(R.string.about_this_wallpaper));

		assertTrue("The about this project title was not found", solo.searchText(ProjectManager.getInstance().getCurrentProject().getName()));
		Log.d("LWP","Project Name is: " + ProjectManager.getInstance().getCurrentProject().getName());
  		if(ProjectManager.getInstance().getCurrentProject().getDescription()!= "")//kein null Pointer 
		{
		  Log.d("LWP","About Description is: " + ProjectManager.getInstance().getCurrentProject().getDescription());
		  assertTrue("The Description of this wallpaper was not found", solo.searchText(solo.getString(ProjectManager.getInstance().getCurrentProject().getDescription())));
		}
		solo.goBack();	
		solo.clickOnText(solo.getString(R.string.main_menu_about_pocketcode));
		solo.sleep(DELAY);
		solo.goBack();
	}
	
	public void testPreferences()
	{
		assertFalse(SoundManager.getInstance().soundDisabledByLwp);
		assertFalse(solo.isCheckBoxChecked(solo.getString(R.string.lwp_allow_sounds)));
		solo.sleep(DELAY);
		solo.clickOnText(solo.getString(R.string.lwp_allow_sounds));
		assertFalse(solo.isCheckBoxChecked(solo.getString(R.string.lwp_allow_sounds)));
		
		solo.clickOnText(solo.getString(R.string.lwp_select_program));
		solo.sleep(DELAY);
		solo.clickOnText(solo.getString(R.string.default_project_name));
		solo.sleep(DELAY);
		solo.clickOnText(solo.getString(R.string.no));
		solo.goBack();

	}
		
    public void testWallpaperSelection()
    {	
		solo.clickOnText(solo.getString(R.string.lwp_select_program));
		solo.sleep(DELAY);
		solo.clickOnText(solo.getString(R.string.default_project_name));
		solo.sleep(DELAY);
		solo.clickOnText(solo.getString(R.string.yes));
    }
    
    public void testDelete()
    {	
    	
    }
	
}
