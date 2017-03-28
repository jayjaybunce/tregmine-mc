package info.tregmine.database;

public interface IContextFactory {
    IContext createContext() throws DAOException;

    void regenerate() throws DAOException;
}
