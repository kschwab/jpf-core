local Enve = import 'enve.libsonnet';

{
  Enve: Enve {
    id+: Enve.NewId('Java PathFinder'),
    extensions+: [
      Enve.NewExtension('openjdk8',
        variables=[
          Enve.NewVariable('BIN', 'bin', path_export=true)
        ]),
    ],
  }
}
