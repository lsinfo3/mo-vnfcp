package de.uniwue.VNFP.gui.tableUI;


import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.plotUI.PlotData;
import de.uniwue.VNFP.model.Objs;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Callback;


import java.util.Comparator;


/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class Table {
    private DataManager dataManager;
    private TableView<TableData> table = new TableView<>();

    public Table(DataManager dataManager) {
        this.dataManager = dataManager;
        initTableData();
    }
    
    private void initTableData(){

    }


    private final ObservableList<TableData> data = FXCollections.observableArrayList();

    public TableView<TableData> initialize() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(
                SelectionMode.MULTIPLE
        );
        table.sortPolicyProperty().set(t -> {
            Comparator<TableData> comparator = (r1, r2)
                    -> r1.isDeleted() ? 1 //rowTotal at the bottom
                    : r2.isDeleted() ? -1 //rowTotal at the bottom
                    :r1.isDeleted()&&r2.isDeleted()? t.getComparator().compare(r1,r2)
                    : t.getComparator() == null ? 0 //no column sorted: don't change order
                    : t.getComparator().compare(r1, r2); //columns are sorted: sort accordingly
            FXCollections.sort(table.getItems(), comparator);
            return true;
        });

        this.data.clear();
        this.data.addAll(this.dataManager.getTableData());
        for (Objs.Obj val:dataManager.getO().values()
                ) {
            String valString = val.toString();
            TableColumn<TableData, Double> tc = new TableColumn<>(valString);
            tc.setCellValueFactory(td -> new SimpleObjectProperty<>(td.getValue().getData().get(valString)));
            if (dataManager.getChecked().contains(valString)) {
                table.getColumns().add(tc);
            }
        }
        table.setItems(data);
        table.setStyle("-fx-selection-bar: red; -fx-selection-bar-non-focused: red;");
        ObservableList<javafx.scene.control.TablePosition> selectedCells = table.getSelectionModel().getSelectedCells();
        final PseudoClass lowPriorityPseudoClass = PseudoClass.getPseudoClass("priority-low");
        table.setRowFactory(new Callback<>() {
            @Override
            public TableRow<TableData> call(TableView<TableData> personTableView) {
                return new TableRow<>() {
                    @Override
                    protected void updateItem(TableData td, boolean b) {
                        super.updateItem(td, b);
                        boolean lowPriority = td != null && td.isDeleted();
                        pseudoClassStateChanged(lowPriorityPseudoClass, lowPriority);
                    }
                };
            }
        });


        selectedCells.addListener((ListChangeListener<javafx.scene.control.TablePosition>) c -> {
            TableData tableData = table.getFocusModel().getFocusedItem();
            for (Cell cells : dataManager.getCells()
                    ) {
                cells.setFillColor(cells.getColor());
                if (!hasResources(cells))cells.setFillColor(Color.DARKGRAY);
            }
            for (PlotData pd : dataManager.getPlotData()
                    ) {
                pd.updateColor(Color.BLACK, 1);
            }
                if (tableData != null) {
                    if (tableData.getDataId() != null) {
                        if (!tableData.isDeleted()) {
                            tableData.paintSolutionNodes();
                            dataManager.getPlotDataWithId(tableData.getDataId()).updateColor(Color.RED, 4);
                            if (dataManager.isDetailsOpen()){
                                dataManager.getGraphUI().getGraph().getMouseGestures().fire();
                            }
                        }
                    }
                }


        });
        table.setOnMousePressed(event -> {
            if (event.isSecondaryButtonDown()) {
                table.getSelectionModel().getSelectedItem();
                dataManager.getConMenu().getContextMenu().show(table,event.getSceneX(),event.getSceneY());
                dataManager.getConMenu().setTableData();
                dataManager.getConMenu().setDataManager(dataManager);
            }
            if (event.isPrimaryButtonDown()){
                dataManager.getConMenu().getContextMenu().hide();
            }
        });
        this.dataManager.setTableView(table);
        table.getSelectionModel().select(0);
        return table;
    }
    private boolean hasResources(Cell cell){
        return cell.hasResources();
    }
    public ObservableList<TableData> getData() {
        return data;
    }

    public TableView<TableData> getTable() {
        return table;
    }


}
