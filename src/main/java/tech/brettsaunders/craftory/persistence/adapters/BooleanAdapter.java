package tech.brettsaunders.craftory.persistence.adapters;


import de.tr7zw.changeme.nbtapi.NBTCompound;
import tech.brettsaunders.craftory.persistence.PersistenceStorage;

public class BooleanAdapter implements DataAdapter<Boolean> {

    @Override
    public void store(PersistenceStorage persistenceStorage, Boolean value, NBTCompound nbtCompound) {
        nbtCompound.setBoolean("boolean", value);
    }

    @Override
    public Boolean parse(PersistenceStorage persistenceStorage, Object parentObject, NBTCompound nbtCompound) {
        if (!nbtCompound.hasKey("boolean")) {
            return null;
        }
        return nbtCompound.getBoolean("boolean");
    }
}