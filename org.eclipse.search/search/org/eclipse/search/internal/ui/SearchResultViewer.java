package org.eclipse.search.internal.ui;

/*
 * Licensed Materials - Property of IBM,
 * WebSphere Studio Workbench
 * (c) Copyright IBM Corp 1999, 2000
 */
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import org.eclipse.search.ui.IContextMenuContributor;
import org.eclipse.search.ui.ISearchResultViewEntry;

/**
 * A special viewer to present search results. The viewer implements an
 * optimized adding and removing strategy. Furthermore it manages
 * contributions for search result types. For example the viewer's context
 * menu differs if the search result has been generated by a text or
 * a java search.
 */
class SearchResultViewer extends TableViewer {
	
	private SearchResultView fOuterPart;
	private boolean fFirstTime= true;
	private ShowNextResultAction fShowNextResultAction;
	private ShowPreviousResultAction fShowPreviousResultAction;
	private GotoMarkerAction fGotoMarkerAction;
	private SearchAgainAction fSearchAgainAction;
	private RemoveAllSearchesAction fRemoveAllSearchesAction;
	private SortDropDownAction fSortDropDownAction;
	private SearchDropDownAction fSearchDropDownAction;
	private int fMarkerToShow;
	
	/*
	 * These static fields will go away when support for 
	 * multiple search will be implemented
	 */
	private SearchResultLabelProvider fLabelProvider;
	private static IContextMenuContributor fgContextMenuContributor;
	private static IAction fgGotoMarkerAction;
	
	public SearchResultViewer(SearchResultView outerPart, Composite parent) {
		super(new Table(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION));
		
		fOuterPart= outerPart;
		Assert.isNotNull(fOuterPart);
		
		setUseHashlookup(true);
		setContentProvider(new SearchResultContentProvider());
		setLabelProvider(new SearchResultLabelProvider());

		boolean hasSearch= SearchManager.getDefault().getCurrentSearch() != null;

		fShowNextResultAction= new ShowNextResultAction(this);
		fShowNextResultAction.setEnabled(false);
		fShowPreviousResultAction= new ShowPreviousResultAction(this);
		fShowPreviousResultAction.setEnabled(false);
		fGotoMarkerAction= new GotoMarkerAction(this);
		fGotoMarkerAction.setEnabled(false);
		fRemoveAllSearchesAction= new RemoveAllSearchesAction();
		fSearchAgainAction= new SearchAgainAction();
		fSearchAgainAction.setEnabled(hasSearch);
		fRemoveAllSearchesAction.setEnabled(hasSearch);
		fSortDropDownAction = new SortDropDownAction(this);
		fSortDropDownAction.setEnabled(getItemCount() > 0);
		fSearchDropDownAction= new SearchDropDownAction(this);
		fSearchDropDownAction.setEnabled(hasSearch);

		addSelectionChangedListener(
			new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					boolean hasSelection= ! getSelection().isEmpty();
					boolean hasElements= getItemCount() > 0;
					fShowNextResultAction.setEnabled(hasSelection || hasElements);
					fShowPreviousResultAction.setEnabled(hasSelection || hasElements);
					fGotoMarkerAction.setEnabled(hasSelection);
					fMarkerToShow= -1;
					String location= "";
					if (hasSelection) {
						ISearchResultViewEntry entry= (ISearchResultViewEntry)getTable().getItem(getTable().getSelectionIndex()).getData();
						IPath path= entry.getResource().getFullPath();
						if (path != null)
							location= path.toString();
					}
					setStatusLineMessage(location);
				}
			}
		);

		getTable().addSelectionListener(new SelectionAdapter() {
			public void widgetDefaultSelected(SelectionEvent e) {
				showResult();
			}
		});
		
		MenuManager menuMgr= new MenuManager("#PopUp");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(
			new IMenuListener() {
				public void menuAboutToShow(IMenuManager mgr) {
					fillContextMenu(mgr);
				}
			});
		Menu menu= menuMgr.createContextMenu(getTable());
		getTable().setMenu(menu);		
	}
	void enableActions() {
		boolean state= getItemCount() > 0;
		if (state != fShowNextResultAction.isEnabled()) {
			fShowNextResultAction.setEnabled(state);
			fShowPreviousResultAction.setEnabled(state);
			fSortDropDownAction.setEnabled(state);
		}
		state= SearchManager.getDefault().getCurrentSearch() != null;
		if (state != fRemoveAllSearchesAction.isEnabled())
			fRemoveAllSearchesAction.setEnabled(state);
			fSearchDropDownAction.setEnabled(state);
		if (fGotoMarkerAction.isEnabled())
			fGotoMarkerAction.setEnabled(false);
		if (state != fSearchAgainAction.isEnabled())
			fSearchAgainAction.setEnabled(state);
	}

	protected void inputChanged(Object input, Object oldInput) {
		getTable().removeAll();
		super.inputChanged(input, oldInput);
		fMarkerToShow= -1;
		updateTitle();
		enableActions();
	}

	//--- Contribution management -----------------------------------------------
	
	void fillContextMenu(IMenuManager menu) {
		if (fgContextMenuContributor != null)
			fgContextMenuContributor.fill(menu, this);
			
		menu.add(new Separator());
		if (! getSelection().isEmpty()) {
			menu.add(new GotoMarkerAction(this));
			menu.add(new RemoveResultAction(this));
			menu.add(new Separator());
		}
		// If we have elements
		if (getItemCount() > 0) {
			menu.add(new RemoveAllResultsAction());
		}
		menu.add(fSearchAgainAction);
		menu.add(fSortDropDownAction);
	}


	IAction getGotoMarkerAction() {
		// null as return value is covered (no action will take place)
		return fgGotoMarkerAction;
	}


	void setGotoMarkerAction(IAction gotoMarkerAction) {
		fgGotoMarkerAction= gotoMarkerAction;
	}


	void setContextMenuTarget(IContextMenuContributor contributor) {
		fgContextMenuContributor= contributor;
	}

	void setPageId(String pageId) {
		fSortDropDownAction.setPageId(pageId);
	}

	void fillToolBar(IToolBarManager tbm) {
		tbm.add(fShowNextResultAction);
		tbm.add(fShowPreviousResultAction);
		tbm.add(fGotoMarkerAction);
		tbm.add(new Separator());
		tbm.add(fRemoveAllSearchesAction);
		tbm.add(fSearchDropDownAction);
		
		// need to hook F5 to table
		getTable().addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.F5)
					fSearchDropDownAction.run();
			}
		});
	}	

	int getItemCount() {
		return SearchManager.getDefault().getCurrentItemCount();
	}

	void internalSetLabelProvider(ILabelProvider provider) {
		IBaseLabelProvider tableLabelProvider= getLabelProvider();
		if (tableLabelProvider instanceof SearchResultLabelProvider)
			((SearchResultLabelProvider)getLabelProvider()).setLabelProvider(provider);
		else {
			// should never happen - just to be safe
			setLabelProvider(new SearchResultLabelProvider());
			internalSetLabelProvider(provider);
		}
	}
	
	/**
	 * Makes the first marker of the current result entry
	 * visible in an editor. If no result
	 * is visible, this method does nothing.
	 */
	public void showResult() {
		Table table= getTable();
		if (!canDoShowResult(table))
			return;

		int index= table.getSelectionIndex();
		SearchResultViewEntry entry= (SearchResultViewEntry)getTable().getItem(index).getData();

		fMarkerToShow= 0;
		entry.setSelectedMarkerIndex(0);
		openCurrentSelection();
	}
	
	/**
	 * Makes the next result (marker) visible in an editor. If no result
	 * is visible, this method makes the first result visible.
	 */
	public void showNextResult() {
		Table table= getTable();
		if (!canDoShowResult(table))
			return;

		int index= table.getSelectionIndex();
		SearchResultViewEntry entry= null;
		if (index > -1)
			entry= (SearchResultViewEntry)table.getItem(index).getData();

		fMarkerToShow++;
		if (entry == null || fMarkerToShow >= entry.getMatchCount()) {
			// move selection
			if (index == -1) {
				index= 0;
			} else {
				index++;
				if (index >= table.getItemCount())
					index= 0;
			}
			fMarkerToShow= 0;
			entry= (SearchResultViewEntry)getTable().getItem(index).getData();
			selectResult(table, index);
		}
		entry.setSelectedMarkerIndex(fMarkerToShow);
		openCurrentSelection();
	}
	
	/**
	 * Makes the previous result (marker) visible. If there isn't any
	 * visible result, this method makes the last result visible.
	 */
	public void showPreviousResult() {
		Table table= getTable();
		if (!canDoShowResult(table))
			return;
				
		int index= table.getSelectionIndex();
		SearchResultViewEntry entry;

		fMarkerToShow--;
		if (fMarkerToShow >= 0)
			entry= (SearchResultViewEntry)getTable().getItem(getTable().getSelectionIndex()).getData();			
		else {
			// move selection		
			int count= table.getItemCount();
			if (index == -1) {
				index= count - 1;
			} else {
				index--;
				if (index < 0)
					index= count - 1;
			}
			entry= (SearchResultViewEntry)getTable().getItem(index).getData();
			fMarkerToShow= entry.getMatchCount() - 1;
			selectResult(table, index);
		}
		entry.setSelectedMarkerIndex(fMarkerToShow);
		openCurrentSelection();
	}
	
	private boolean canDoShowResult(Table table) {
		if (table == null || getItemCount() == 0)
			return false;
		return true;			
	}
		
	private void selectResult(Table table, int index) {
		TableItem item= table.getItem(index);
		table.setSelection(index);
		table.showSelection();
	}

	private void openCurrentSelection() {
		IAction action= getGotoMarkerAction();
		if (action != null)
			action.run();
	}
	
	/**
	 * Update the title
	 */
	protected void updateTitle() {
		int count= SearchManager.getDefault().getCurrentItemCount();
		boolean showZero= SearchManager.getDefault().getCurrentSearch() != null;
		String title= SearchPlugin.getResourceString("SearchResultView.title");
		if (count > 0 || showZero)
			title= title + " (" + count + " " + SearchPlugin.getResourceString("SearchResultView.matches") + ")";
		if (!title.equals(fOuterPart.getTitle()))
			fOuterPart.setTitle(title);
	}

	/**
	 * Sets the message text to be displayed on the status line.
	 * The image on the status line is cleared.
	 */
	private void setStatusLineMessage(String message) {
		fOuterPart.getViewSite().getActionBars().getStatusLineManager().setMessage(message);
	}

	protected void handleDispose(DisposeEvent event) {
		Menu menu= getTable().getMenu();
		if (menu != null)
			menu.dispose();
		super.handleDispose(event);
	}

	//--- Change event handling -------------------------------------------------
	
	/**
	 * Handle a single add.
	 */
	protected void handleAddMatch(ISearchResultViewEntry entry) {
		insert(entry, -1);
	}
	
	/**
	 * Handle a single remove.
	 */
	protected void handleRemoveMatch(ISearchResultViewEntry entry) {
		Widget item= findItem(entry);
		if (entry.getMatchCount() == 0) {
			if (item instanceof TableItem) {
				TableItem ti= (TableItem)item;
				disassociate(ti);
				ti.dispose();
			}
		}
		else
			updateItem(item, entry);
	}
	
	/**
	 * Handle remove all.
	 */
	protected void handleRemoveAll() {
		setInput(null);
	}
	
	/**
	 * Handle an update of an entry.
	 */
	protected void handleUpdateMatch(ISearchResultViewEntry entry) {
		Widget item= findItem(entry);
		updateItem(item, entry);
	}
}
