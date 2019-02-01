package de.uniwue.VNFP.gui.tableUI;


import de.uniwue.VNFP.gui.core.DataManager;
import de.uniwue.VNFP.gui.graphUI.cell.Cell;
import de.uniwue.VNFP.gui.plotUI.PlotData;
import de.uniwue.VNFP.model.Node;
import de.uniwue.VNFP.model.Objs;
import de.uniwue.VNFP.model.solution.Solution;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Created by Simon Raffeck on 06.11.17.
 */
public class TableData {
    private Integer dataId = 0;

    private boolean deleted = false;


    private ObservableMap<String,Double> data = FXCollections.observableHashMap();


    private DataManager dataManager;
    private Solution solution;

    public TableData(DataManager dataManager, Solution solution) {
        this.dataManager = dataManager;
        this.solution = solution;
        initValues();
    }

    public void initValues(){
        for (Objs.Obj val:dataManager.getO().values()
             ) {
            data.put(val.toString(),this.dataManager.round(solution.vals[val.i],2));
        }
    }

    public void setDataId(int id) {
        this.dataId = id;
    }

    public Integer getDataId() {
        return this.dataId;
    }

    void paintSolutionNodes() {
        for (Node node : solution.graph.getNodes().values()
                ) {
            if (solution.nodeMap.get(node).getAssignments().length != 0) {
                for (Cell cell : dataManager.getCells()
                        ) {
                    if (cell.getCellId().equals(node.name)) {
                        cell.setIndicator();
                    }
                }
            }
        }
    }

    public Solution getSolution() {
        return solution;
    }

    public ObservableMap<String, Double> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TableData tableData = (TableData) o;

        if (!dataId.equals(tableData.dataId)) return false;
        if (!data.equals(tableData.data)) return false;
        return dataManager.equals(tableData.dataManager) && solution.equals(tableData.solution);
    }

    @Override
    public int hashCode() {
        int result = dataId.hashCode();
        result = 31 * result + data.hashCode();
        result = 31 * result + dataManager.hashCode();
        result = 31 * result + solution.hashCode();
        return result;
    }
    public void delte(){
        if (!this.isDeleted()) {
            PlotData toDelete = dataManager.getPlotDataWithId(this.dataId);
            dataManager.getPlotData().remove(toDelete);
            dataManager.getTableData().remove(this);
            this.setDeleted(true);
            dataManager.getTableData().add(this);
            dataManager.reInitiate();
            dataManager.getTableUI().getTable().sort();
        }
    }
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
