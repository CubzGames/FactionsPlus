package markehme.factionsplus.MCore;

import markehme.factionsplus.FactionsPlus;

import com.massivecraft.mcore.MCore;
import com.massivecraft.mcore.store.Coll;
import com.massivecraft.mcore.store.MStore;

public class LConfColl extends Coll<LConf> {
	// -------------------------------------------- //
	// INSTANCE & CONSTRUCT
	// -------------------------------------------- //

	private static LConfColl i = new LConfColl();
	public static LConfColl get() { return i; }
	private LConfColl() {
		super(Const.COLLECTION_LCONF, LConf.class, MStore.getDb(), FactionsPlus.instance);
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //

	@Override
	public void init() {
		super.init();
		LConf.i = this.get(MCore.INSTANCE, true);
	}

}
