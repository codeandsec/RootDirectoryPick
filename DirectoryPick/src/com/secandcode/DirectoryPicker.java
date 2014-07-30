/**
 * By SecAndCode.com
 * 
 * Based on http://bgreco.net/directorypicker/
 * 
 * 
 * Added: root access feature for directory browsing
 * Added: Icon for files and folder
 * Added: showing known file types with icons. Icons from https://github.com/teambox/Free-file-icons
 * Added: file choosing feature
 * and some other minor fixes and changes
 */

package com.secandcode;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import com.secandcode.directorypick.R;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

/**
 * Copyright (C) 2011 by Brad Greco <brad@bgreco.net>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

public class DirectoryPicker extends ListActivity {

	public static final String START_DIR = "startDir";
	public static final String USE_ROOT = "useRoot";
	public static final String ONLY_DIRS = "onlyDirs";
	public static final String SHOW_HIDDEN = "showHidden";
	public static final String CHOSEN_DIRECTORY = "chosenDir";
	public static final int PICK_DIRECTORY = 61653;
	public String TheDir;
	private File dir;
	private boolean showHidden = true;
	private boolean onlyDirs = true;
	private boolean useRoot = false;
	private ArrayList<File> files;
	private List<FileEntry> rfiles = null;
	/** Entry type: File */
	public static final int TYPE_FILE = 0;
	/** Entry type: Directory */
	public static final int TYPE_DIRECTORY = 1;
	/** Entry type: Directory Link */
	public static final int TYPE_DIRECTORY_LINK = 2;
	/** Entry type: Block */
	public static final int TYPE_BLOCK = 3;
	/** Entry type: Character */
	public static final int TYPE_CHARACTER = 4;
	/** Entry type: Link */
	public static final int TYPE_LINK = 5;
	/** Entry type: Socket */
	public static final int TYPE_SOCKET = 6;
	/** Entry type: FIFO */
	public static final int TYPE_FIFO = 7;
	/** Entry type: Other */
	public static final int TYPE_OTHER = 8;
	/** Device side file separator. */
	public static final String FILE_SEPARATOR = "/";
	private static Pattern sLsPattern = Pattern
			.compile("^([bcdlsp-][-r][-w][-xsS][-r][-w][-xsS][-r][-w][-xstST])\\s+(\\S+)\\s+(\\S+)\\s+([\\d\\s,]*)\\s+(\\d{4}-\\d\\d-\\d\\d)\\s+(\\d\\d:\\d\\d)\\s+(.*)$");

	private Integer ChooseImage(String extension) {
		String ext = null;
		int i = extension.lastIndexOf('.');
		if (i > 0) {
			ext = extension.substring(i + 1);
		}
		int ix = getResources().getIdentifier("drawable/" + ext, "drawable",
				getPackageName());
		if (ix == 0)
			ix = R.drawable._blank;
		return ix;
	}

	public String RunAsRoot(String cmdline) {
		String result = "";
		try {
			String line;
			Process process = Runtime.getRuntime().exec("su");
			OutputStream stdin = process.getOutputStream();
			InputStream stdout = process.getInputStream();

			stdin.write((cmdline + "\n").getBytes());
			stdin.write("exit\n".getBytes());
			stdin.flush();
			stdin.close();
			
			BufferedReader br = new BufferedReader(
					new InputStreamReader(stdout));
			
			while ((( line = br.readLine()) != null)  && (line.trim().length() > 1)) {
				result += line + "\n";
				 if (br.ready() == false) break;
			}
			br.close();


			process.waitFor();
			process.destroy();
			return result;

		} catch (Exception ex) {
		}
		return "";
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle extras = getIntent().getExtras();

		dir = Environment.getExternalStorageDirectory();
		if (extras != null) {
			String preferredStartDir = extras.getString(START_DIR);

			TheDir = preferredStartDir;
			showHidden = extras.getBoolean(SHOW_HIDDEN, true);
			onlyDirs = extras.getBoolean(ONLY_DIRS, true);
			useRoot = extras.getBoolean(USE_ROOT, false);

			if (preferredStartDir != null) {
				File startDir = new File(preferredStartDir);
				if (startDir.isDirectory()) {
					dir = startDir;
				}
			}
		}

		if (useRoot == false && !dir.canRead()) {
			Context context = getApplicationContext();
			String msg = "Could not read folder contents.";
			Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
			toast.show();
			return;
		}

		setContentView(R.layout.chooser_list);
		setTitle(dir.getAbsolutePath());
		Button btnChoose = (Button) findViewById(R.id.btnChoose);
		if (!onlyDirs)
			btnChoose.setVisibility(View.GONE);
		String name = dir.getName();
		if (name.length() == 0)
			name = "/";
		btnChoose.setText("Choose " + "'" + name + "'");
		btnChoose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				returnDir(dir.getAbsolutePath());
			}
		});

		ListView lv = getListView();

		String[] names = null;

		if (useRoot) {

			rfiles = processNewLines(TheDir);

			names = rnames(rfiles);

		} else {

			files = filter(dir.listFiles(), onlyDirs, showHidden);
			names = names(files);
		}

		List<HashMap<String, String>> listinfo = new ArrayList<HashMap<String, String>>();
		listinfo.clear();
		for (int i = 0; i < names.length; i++) {
			HashMap<String, String> hm = new HashMap<String, String>();

			if (useRoot) {
				if (rfiles.get(i).type == TYPE_DIRECTORY
						|| rfiles.get(i).type == TYPE_DIRECTORY_LINK) {
					hm.put("name", names[i]);
					hm.put("image", Integer.toString(R.drawable.folder));
					listinfo.add(hm);
				} else {
					hm.put("name", names[i]);
					hm.put("image", Integer.toString(ChooseImage(names[i])));
					listinfo.add(hm);
				}
			} else { // not root
				if (files.get(i).isDirectory()) {
					hm.put("name", names[i]);
					hm.put("image", Integer.toString(R.drawable.folder));
					listinfo.add(hm);
				} else {
					hm.put("name", names[i]);
					hm.put("image", Integer.toString(ChooseImage(names[i])));
					listinfo.add(hm);
				}
			}

		}

		String[] from = { "image", "name" };
		int[] to = { R.id.img, R.id.txt };
		SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), listinfo,
				R.layout.list_layout, from, to);
		lv.setAdapter(adapter);
		lv.setTextFilterEnabled(true);

		lv.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {

				if (useRoot) {

					if (rfiles.get(position).type == TYPE_FILE) {
						String fullpath = rfiles.get(position).AbsolutePath;
						returnDir(fullpath);
					}

					if (rfiles.get(position).type != TYPE_DIRECTORY
							&& rfiles.get(position).type != TYPE_DIRECTORY_LINK)
						return;

					String path = rfiles.get(position).AbsolutePath;
					Intent intent = new Intent(DirectoryPicker.this,
							DirectoryPicker.class);
					intent.putExtra(DirectoryPicker.USE_ROOT, true);
					intent.putExtra(DirectoryPicker.START_DIR, path);
					intent.putExtra(DirectoryPicker.SHOW_HIDDEN, showHidden);
					intent.putExtra(DirectoryPicker.ONLY_DIRS, onlyDirs);
					startActivityForResult(intent, PICK_DIRECTORY);
				} else {

					if (files.get(position).isFile()) {
						String fullpath = files.get(position).getAbsolutePath();
						returnDir(fullpath);

					}
					if (!files.get(position).isDirectory())
						return;
					String path = files.get(position).getAbsolutePath();
					Intent intent = new Intent(DirectoryPicker.this,
							DirectoryPicker.class);
					intent.putExtra(DirectoryPicker.START_DIR, path);
					intent.putExtra(DirectoryPicker.USE_ROOT, false);
					intent.putExtra(DirectoryPicker.SHOW_HIDDEN, showHidden);
					intent.putExtra(DirectoryPicker.ONLY_DIRS, onlyDirs);
					startActivityForResult(intent, PICK_DIRECTORY);
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_DIRECTORY && resultCode == RESULT_OK) {
			Bundle extras = data.getExtras();
			String path = (String) extras.get(DirectoryPicker.CHOSEN_DIRECTORY);
			returnDir(path);
		}
	}

	private void returnDir(String path) {
		Intent result = new Intent();
		path = path.replaceAll("//", "/");
		result.putExtra(CHOSEN_DIRECTORY, path);
		setResult(RESULT_OK, result);
		finish();
	}

	public List<FileEntry> processNewLines(String InitPath) {
		String[] lines = null;
		List<FileEntry> listOfFiles = new ArrayList<FileEntry>();
		String tmpcommand = "ls -l " + InitPath + "/";
		tmpcommand.replaceAll("//", "/");
		String tmp = null;
		tmp = RunAsRoot(tmpcommand);
		if (tmp.equals("") || tmp == null)
			return listOfFiles;
		lines = tmp.split("\n");
		
		for (String line : lines) {
			// no need to handle empty lines.
			if (line.length() == 0) {
				continue;
			}
			// run the line through the regexp
			Matcher m = sLsPattern.matcher(line);
			if (m.matches() == false) {
				continue;
			}
			// get the nam
			String name = m.group(7);
			// get the rest of the groups
			String permissions = m.group(1);
			String owner = m.group(2);
			String group = m.group(3);
			String size = m.group(4);
			String date = m.group(5);
			String time = m.group(6);
			String info = null;
			// and the type
			int objectType = TYPE_OTHER;
			switch (permissions.charAt(0)) {
			case '-':
				objectType = TYPE_FILE;
				break;
			case 'b':
				objectType = TYPE_BLOCK;
				break;
			case 'c':
				objectType = TYPE_CHARACTER;
				break;
			case 'd':
				objectType = TYPE_DIRECTORY;
				break;
			case 'l':
				objectType = TYPE_LINK;
				break;
			case 's':
				objectType = TYPE_SOCKET;
				break;
			case 'p':
				objectType = TYPE_FIFO;
				break;
			}
			// now check what we may be linking to
			if (objectType == TYPE_LINK) {
				String[] segments = name.split("\\s->\\s"); //$NON-NLS-1$
				// we should have 2 segments
				if (segments.length == 2) {
					// update the entry name to not contain the link
					name = segments[0];
					// and the link name
					info = segments[1];
					// now get the path to the link
					String[] pathSegments = info.split(FILE_SEPARATOR);
					if (pathSegments.length == 1) {
						// the link is to something in the same directory,
						// unless the link is ..
						if ("..".equals(pathSegments[0])) { //$NON-NLS-1$
							// set the type and we're done.
							objectType = TYPE_DIRECTORY_LINK;
						} else {
							// either we found the object already
							// or we'll find it later.
						}
					}
				}
				// add an arrow in front to specify it's a link.
				info = "-> " + info; //$NON-NLS-1$;
			}
		
			if (onlyDirs) {
				if (objectType == TYPE_DIRECTORY) {
					FileEntry entry = new FileEntry();
					entry.name = name;
					entry.AbsolutePath = InitPath + "/" + name;
					entry.permissions = permissions;
					entry.size = size;
					entry.date = date;
					entry.time = time;
					entry.owner = owner;
					entry.group = group;
					entry.type = objectType;
					if (objectType == TYPE_LINK) {
						entry.info = info;
					}
					listOfFiles.add(entry);
				}

			} else {
				FileEntry entry = new FileEntry();
				entry.name = name;
				entry.AbsolutePath = InitPath + "/" + name;
				entry.permissions = permissions;
				entry.size = size;
				entry.date = date;
				entry.time = time;
				entry.owner = owner;
				entry.group = group;
				entry.type = objectType;
				if (objectType == TYPE_LINK) {
					entry.info = info;
				}
				listOfFiles.add(entry);
			}
		}
		return listOfFiles;
	}

	public final static class FileEntry {

		String AbsolutePath;
		String name;
		String info;
		String permissions;
		String size;
		String date;
		String time;
		String owner;
		String group;
		int type;
	}

	public ArrayList<File> filter(File[] file_list, boolean onlyDirs,
			boolean showHidden) {
		ArrayList<File> files = new ArrayList<File>();
		for (File file : file_list) {
			if (onlyDirs && !file.isDirectory())
				continue;
			if (!showHidden && file.isHidden())
				continue;
			files.add(file);
		}
		Collections.sort(files);

		return files;
	}

	public String[] rnames(List<FileEntry> files) {
		String[] names = new String[files.size()];
		int i = 0;
		for (FileEntry file : files) {
			names[i] = file.name;
			i++;
		}
		return names;
	}

	public String[] names(ArrayList<File> files) {
		String[] names = new String[files.size()];
		int i = 0;
		for (File file : files) {
			names[i] = file.getName();
			i++;
		}
		return names;
	}
}
